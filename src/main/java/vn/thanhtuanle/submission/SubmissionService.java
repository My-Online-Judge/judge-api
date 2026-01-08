package vn.thanhtuanle.submission;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.thanhtuanle.entity.Language;
import vn.thanhtuanle.entity.Problem;
import vn.thanhtuanle.entity.Submission;
import vn.thanhtuanle.judge.JudgeService;
import vn.thanhtuanle.judge.dto.JudgeResponseDto;
import vn.thanhtuanle.judge.dto.JudgeResultDto;
import vn.thanhtuanle.submission.dto.SubmissionRequestDto;
import vn.thanhtuanle.submission.dto.SubmissionResponseDto;
import vn.thanhtuanle.submission.mapper.SubmissionMapper;
import vn.thanhtuanle.problem.ProblemRepository;
import vn.thanhtuanle.language.LanguageRepository;
import vn.thanhtuanle.common.enums.SubmissionStatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

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

    @Transactional
    public SubmissionResponseDto submit(SubmissionRequestDto req) {
        log.info("Start submission service for problem: {}", req.getProblemSlug());
        log.info("Validating source code");
        judgeService.validate(req.getSourceCode());

        Problem problem = problemRepository.findByProblemSlug(req.getProblemSlug())
                .orElseThrow(() -> new IllegalArgumentException("Problem not found"));

        Language language = languageRepository.findByIdentifier(req.getLanguageIdentifier())
                .orElseThrow(() -> new IllegalArgumentException("Language not found"));

        Submission submission = createPendingSubmission(req, problem, language);
        submissionRepository.save(submission);

        log.info("Start calling judge service");
        List<JudgeResultDto> details = handleJudgeResponse(submission, problem, language);

        submissionRepository.save(submission);

        log.info("End submission service for problem: {}", req.getProblemSlug());
        return submissionMapper.toDto(submission, details);
    }

    private Submission createPendingSubmission(SubmissionRequestDto req, Problem problem, Language language) {
        return Submission.builder()
                .sourceCode(req.getSourceCode())
                .problem(problem)
                .language(language)
                .status(SubmissionStatus.PENDING.getValue())
                .time(0)
                .memory(0L)
                .build();
    }

    private List<JudgeResultDto> handleJudgeResponse(Submission submission, Problem problem, Language language) {
        try {
            JudgeResponseDto judgeResponse = judgeService.judge(submission.getSourceCode(), problem, language);
            return processJudgeResponse(submission, judgeResponse);
        } catch (Exception e) {
            log.error("Error during judging", e);
            submission.setStatus(SubmissionStatus.ERROR.getValue());
            submission.setErrorMessage(e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<JudgeResultDto> processJudgeResponse(Submission submission, JudgeResponseDto judgeResponse) {
        List<JudgeResultDto> details = new ArrayList<>();
        if (judgeResponse.getErr() != null) {
            log.info("Judge service returned error: {}", judgeResponse.getErr());
            submission.setStatus(SubmissionStatus.ERROR.getValue());
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
                submission.setStatus(SubmissionStatus.JUDGED.getValue());
            } else {
                log.info("Judge service returned non-array data, marking as error");
                submission.setStatus(SubmissionStatus.ERROR.getValue());
                submission.setErrorMessage(judgeResponse.getData().asText());
            }
        } else {
            log.info("Judge service returned unknown response, marking as error");
            submission.setStatus(SubmissionStatus.ERROR.getValue());
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

        return submissionRepository.findByProblemSlug(problemSlug).stream()
                .map(submissionMapper::toDto)
                .toList();
    }
}
