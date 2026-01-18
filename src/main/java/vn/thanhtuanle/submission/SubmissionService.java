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
                .shareSubmission(req.getShareSubmission())
                .build();
    }

    private List<JudgeResultDto> handleJudgeResponse(Submission submission, Problem problem, Language language) {
        try {
            JudgeResponseDto judgeResponse = judgeService.judge(submission.getSourceCode(), problem, language);
            return processJudgeResponse(submission, judgeResponse);
        } catch (Exception e) {
            log.error("Error during judging", e);
            submission.setStatus(SubmissionResult.WRONG_ANSWER.getValue());
            submission.setErrorMessage(e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<JudgeResultDto> processJudgeResponse(Submission submission, JudgeResponseDto judgeResponse) {
        List<JudgeResultDto> details = new ArrayList<>();
        if (judgeResponse.getErr() != null) {
            log.info("Judge service returned error: {}", judgeResponse.getErr());
            submission.setStatus(SubmissionResult.WRONG_ANSWER.getValue());
            String extraInfo = judgeResponse.getData() != null ? judgeResponse.getData().asText() : "";
            submission.setErrorMessage(judgeResponse.getErr() + (extraInfo.isEmpty() ? "" : ": " + extraInfo));
        } else if (judgeResponse.getData() != null) {
            if (judgeResponse.getData().isArray()) {
                log.info("Processing judge results");
                int maxCpuTime = 0;
                int maxRealTime = 0;
                long maxMemory = 0;
                int finalResult = 0;

                for (JsonNode node : judgeResponse.getData()) {
                    JudgeResultDto item = objectMapper.convertValue(node, JudgeResultDto.class);

                    if (item.getCpuTime() != null)
                        maxCpuTime = Math.max(maxCpuTime, item.getCpuTime());
                    if (item.getRealTime() != null)
                        maxRealTime = Math.max(maxRealTime, item.getRealTime());
                    if (item.getMemory() != null)
                        maxMemory = Math.max(maxMemory, item.getMemory());

                    // Pick first non-zero result as the overall result
                    if (item.getResult() != null && item.getResult() != 0) {
                        if (finalResult == 0) {
                            finalResult = item.getResult();
                        }
                    }

                    details.add(item);
                }
                submission.setCpuTime(maxCpuTime);
                submission.setTime(maxRealTime);
                submission.setMemory(maxMemory);
                submission.setResult(finalResult);
                submission.setStatus(finalResult == 0 ? SubmissionResult.SUCCESS.getValue()
                        : SubmissionResult.WRONG_ANSWER.getValue());
            } else {
                log.info("Judge service returned non-array data, marking as error");
                submission.setStatus(SubmissionResult.WRONG_ANSWER.getValue());
                submission.setErrorMessage(judgeResponse.getData().asText());
            }
        } else {
            log.info("Judge service returned unknown response, marking as error");
            submission.setStatus(SubmissionResult.WRONG_ANSWER.getValue());
            submission.setErrorMessage("Unknown judge response");
        }

        return details;
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponseDto> getSubmissionsByProblemSlug(String problemSlug) {
        log.info("Service to get submissions by problem slug: {}", problemSlug);

        if (!problemRepository.existsByProblemSlug(problemSlug)) {
            throw new IllegalArgumentException("Problem not found with slug: " + problemSlug);
        }

        return submissionRepository.findByProblemSlugOrderByCreatedAtDesc(problemSlug).stream()
                .map(submissionMapper::toDto)
                .toList();
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
