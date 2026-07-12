package vn.thanhtuanle.submission;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.thanhtuanle.common.constant.AppProperties;
import vn.thanhtuanle.common.enums.SubmissionResult;
import vn.thanhtuanle.common.exception.ResourceNotFoundException;
import vn.thanhtuanle.common.payload.PageResponse;
import vn.thanhtuanle.entity.Language;
import vn.thanhtuanle.entity.Problem;
import vn.thanhtuanle.entity.Submission;
import vn.thanhtuanle.entity.User;
import vn.thanhtuanle.judge.JudgeService;
import vn.thanhtuanle.judge.dto.JudgeResponseDto;
import vn.thanhtuanle.judge.dto.JudgeResultDto;
import vn.thanhtuanle.submission.dto.SubmissionRequestDto;
import vn.thanhtuanle.submission.dto.SubmissionResponseDto;
import vn.thanhtuanle.submission.mapper.SubmissionMapper;
import vn.thanhtuanle.problem.ProblemRepository;
import vn.thanhtuanle.language.LanguageRepository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.thanhtuanle.user.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final JudgeService judgeService;
    private final ProblemRepository problemRepository;
    private final LanguageRepository languageRepository;
    private final ObjectMapper objectMapper;
    private final SubmissionMapper submissionMapper;
    private final UserService userService;

    @Transactional
    public SubmissionResponseDto submit(SubmissionRequestDto req) {
        log.info("Start submission service for problem: {}", req.getProblemSlug());
        log.info("Validating source code");
        judgeService.validate(req.getSourceCode());

        Problem problem = problemRepository.findByProblemSlug(req.getProblemSlug())
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found"));

        Language language = languageRepository.findByIdentifier(req.getLanguageIdentifier())
                .orElseThrow(() -> new ResourceNotFoundException("Language not found"));

        User currentUser = userService.getCurrentUser();

        Submission submission = createPendingSubmission(req, problem, language, currentUser);
        submissionRepository.save(submission);

        log.info("Start calling judge service");
        List<JudgeResultDto> details = handleJudgeResponse(submission, problem, language);

        submissionRepository.save(submission);

        log.info("End submission service for problem: {}", req.getProblemSlug());
        return submissionMapper.toDto(submission, details);
    }

    private Submission createPendingSubmission(SubmissionRequestDto req, Problem problem, Language language,
            User user) {
        return Submission.builder()
                .sourceCode(req.getSourceCode())
                .problem(problem)
                .user(user)
                .language(language)
                .time(0)
                .memory(0L)
                .status(SubmissionResult.PENDING.getValue())
                .shareSubmission(req.getShareSubmission())
                .build();
    }

    private List<JudgeResultDto> handleJudgeResponse(Submission submission, Problem problem, Language language) {
        try {
            JudgeResponseDto judgeResponse = judgeService.judge(submission.getSourceCode(), problem, language);
            return processJudgeResponse(submission, judgeResponse);
        } catch (Exception e) {
            // A failure here is OUR system failing to judge, not the user's code being wrong.
            log.error("Error during judging", e);
            submission.setStatus(SubmissionResult.SYSTEM_ERROR.getValue());
            submission.setErrorMessage(e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<JudgeResultDto> processJudgeResponse(Submission submission, JudgeResponseDto judgeResponse) {
        List<JudgeResultDto> details = new ArrayList<>();

        // Case 1: envelope-level error (e.g. {"err":"CompileError","data":"..."}).
        if (judgeResponse.getErr() != null) {
            log.info("Judge service returned error: {}", judgeResponse.getErr());
            SubmissionResult status = mapEnvelopeError(judgeResponse.getErr());
            submission.setStatus(status.getValue());
            String extraInfo = judgeResponse.getData() != null ? judgeResponse.getData().asText() : "";
            submission.setErrorMessage(judgeResponse.getErr() + (extraInfo.isEmpty() ? "" : ": " + extraInfo));
            return details;
        }

        // Case 2: per-testcase result array.
        if (judgeResponse.getData() != null && judgeResponse.getData().isArray()) {
            log.info("Processing judge results");
            int maxCpuTime = 0;
            int maxRealTime = 0;
            long maxMemory = 0;
            int rawResult = 0; // first failing testcase's raw judge_server result code

            for (JsonNode node : judgeResponse.getData()) {
                JudgeResultDto item = objectMapper.convertValue(node, JudgeResultDto.class);

                if (item.getCpuTime() != null)
                    maxCpuTime = Math.max(maxCpuTime, item.getCpuTime());
                if (item.getRealTime() != null)
                    maxRealTime = Math.max(maxRealTime, item.getRealTime());
                if (item.getMemory() != null)
                    maxMemory = Math.max(maxMemory, item.getMemory());

                if (rawResult == 0 && item.getResult() != null && item.getResult() != 0) {
                    rawResult = item.getResult();
                }

                details.add(item);
            }

            submission.setCpuTime(maxCpuTime);
            submission.setTime(maxRealTime);
            submission.setMemory(maxMemory);
            submission.setResult(rawResult);
            submission.setStatus(resolveVerdict(details).getValue());
            return details;
        }

        // Case 3: anything else (null / non-array data) is a malformed response from our side.
        log.info("Judge service returned unexpected response, marking as system error");
        submission.setStatus(SubmissionResult.SYSTEM_ERROR.getValue());
        submission.setErrorMessage(
                judgeResponse.getData() != null ? judgeResponse.getData().asText() : "Unknown judge response");
        return details;
    }

    /**
     * Map a judge_server envelope error class name to a verdict.
     * Per spec, the only envelope error that is the user's fault is a compile error;
     * every other envelope error (TokenVerificationFailed, JudgeClientError, ...) is ours.
     */
    private SubmissionResult mapEnvelopeError(String err) {
        if ("CompileError".equals(err) || "SPJCompileError".equals(err)) {
            return SubmissionResult.COMPILE_ERROR;
        }
        return SubmissionResult.SYSTEM_ERROR;
    }

    /**
     * Decide the final verdict for a submission from its per-testcase results (ACM mode).
     *
     * Each {@link JudgeResultDto} carries two INDEPENDENT fields:
     *   - error  : libjudger sandbox status. 0 = ran fine; any non-zero (-1..-11, e.g.
     *              EXECVE_FAILED, SPJ_ERROR) means the sandbox/system itself failed.
     *   - result : the verdict for that testcase. 0 = ACCEPTED, otherwise one of
     *              WRONG_ANSWER(-1)/CPU_TLE(1)/REAL_TLE(2)/MLE(3)/RUNTIME_ERROR(4)/SYSTEM_ERROR(5).
     *
     * Policy: a sandbox error anywhere -> SYSTEM_ERROR (overrides all); otherwise the first
     * failing testcase's result wins; all-pass -> ACCEPTED. For OI-mode / SPJ partial scoring
     * you would additionally return PARTIALLY_ACCEPTED here.
     *
     * @return the canonical {@link SubmissionResult} to store in Submission.status.
     */
    private SubmissionResult resolveVerdict(List<JudgeResultDto> details) {
        SubmissionResult verdict = SubmissionResult.ACCEPTED;

        for (JudgeResultDto item : details) {
            // 1. A sandbox/libjudger failure (error != 0) is a system-level problem,
            //    not the user's fault — it overrides everything else immediately.
            if (item.getError() != null && item.getError() != 0) {
                return SubmissionResult.SYSTEM_ERROR;
            }

            // 2. ACM-mode (QingdaoU): the verdict is the FIRST failing testcase's result.
            //    Keep scanning afterwards only to catch a later SYSTEM_ERROR (handled above).
            if (verdict == SubmissionResult.ACCEPTED
                    && item.getResult() != null && item.getResult() != 0) {
                verdict = SubmissionResult.fromValue(item.getResult());
            }
        }

        // 3. No failing testcase and no sandbox error -> ACCEPTED.
        return verdict;
    }

    @Transactional(readOnly = true)
    public PageResponse<SubmissionResponseDto> getSubmissionsByProblemSlug(String problemSlug, int page, int size) {
        log.info("Service to get submissions by problem slug: {}", problemSlug);

        if (!problemRepository.existsByProblemSlug(problemSlug)) {
            throw new IllegalArgumentException("Problem not found with slug: " + problemSlug);
        }

        Pageable pageable = PageRequest.of(page, size);
        return PageResponse.of(submissionRepository
                .findByProblemSlugOrderByCreatedAtDesc(problemSlug, pageable)
                .map(submissionMapper::toDto));
    }

    @Transactional(readOnly = true)
    public SubmissionResponseDto getSubmissionById(String id) {
        log.info("Service to get submission by id: {}", id);
        Submission submission = submissionRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with id: " + id));
        return submissionMapper.toDto(submission);
    }

    @Transactional(readOnly = true)
    public PageResponse<SubmissionResponseDto> getSubmissionsByUser(String userId, int page, int size) {
        log.info("Service to get submissions by user_id: {}", userId);

        Pageable pageable = PageRequest.of(page, size);

        Page<Submission> submissionPage = submissionRepository.findByUserIdOrderByCreatedAtDesc(UUID.fromString(userId),
                pageable);

        log.info("Found {} submissions for user_id: {}", submissionPage.getTotalElements(), userId);
        return PageResponse.of(submissionPage.map(submissionMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<SubmissionResponseDto> getSubmissionsByUserAndProblem(String userId, String problemSlug,
            int page, int size) {
        log.info("Service to get submissions by user_id: {} and problem_slug: {}", userId, problemSlug);

        Pageable pageable = PageRequest.of(page, size);

        Page<Submission> submissionPage = submissionRepository
                .findByUserIdAndProblemSlugOrderByCreatedAtDesc(UUID.fromString(userId), problemSlug, pageable);

        log.info("Found {} submissions for user_id: {} and problem_slug: {}", submissionPage.getTotalElements(), userId,
                problemSlug);
        return PageResponse.of(submissionPage.map(submissionMapper::toDto));
    }
}
