package vn.thanhtuanle.judge;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import vn.thanhtuanle.common.constant.AppProperties;
import vn.thanhtuanle.common.enums.SubmissionResult;
import vn.thanhtuanle.common.enums.SubmissionStatus;
import vn.thanhtuanle.submission.SubmissionRepository;
import vn.thanhtuanle.entity.Submission;
import vn.thanhtuanle.judge.dto.JudgeResponseDto;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JudgeService {

    @Value("${judge.server.url}")
    private String judgeServerUrl;

    @Value("${judge.server.token}")
    private String judgeServerToken;

    private final RestClient.Builder restClientBuilder;
    private final SubmissionRepository submissionRepository;

    private static final int MAX_CODE_LENGTH = 10240;
    private static final List<String> BLACKLISTED_KEYWORDS = List.of(
            "Runtime.getRuntime().exec",
            "/bin/sh",
            "ProcessBuilder",
            "java.lang.Runtime");

    public JudgeResponseDto submitCode(JudgeSubmissionDto submissionDto) {
        String src = submissionDto.getSrc();

        if (src == null || src.isEmpty()) {
            return JudgeResponseDto.builder().err("Source code cannot be empty").build();
        }

        if (src.length() > MAX_CODE_LENGTH) {
            return JudgeResponseDto.builder().err("Source code exceeds maximum length of 10KB").build();
        }

        for (String keyword : BLACKLISTED_KEYWORDS) {
            if (src.contains(keyword)) {
                return JudgeResponseDto.builder().err("Source code contains malicious keyword: " + keyword).build();
            }
        }

        // Create and save PENDING submission
        Submission submission = Submission.builder()
                .problemId(submissionDto.getTestCaseId()) // Using testCaseId as problemId for now based on DTO
                .userId(submissionDto.getUserId())
                .sourceCode(src)
                .status(SubmissionStatus.PENDING.getValue())
                .build();

        submission = submissionRepository.save(submission);

        RestClient restClient = restClientBuilder.build();
        JudgeResponseDto response = restClient.post()
                .uri(judgeServerUrl + AppProperties.JUDGE_SERVER_ENDPOINT)
                .header(AppProperties.X_JUDGE_SERVER_TOKEN, judgeServerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(submissionDto)
                .retrieve()
                .body(JudgeResponseDto.class);

        // Update submission with result
        if (response != null && response.getErr() == null) {
            submission.setStatus(SubmissionStatus.JUDGED.getValue()); // Status is JUDGED, result needs parsing from
                                                                      // data
            // For now, assume success if no error
            submission.setResult(SubmissionResult.OK.getValue());
        } else {
            submission.setStatus(SubmissionStatus.ERROR.getValue());
            submission.setResult(SubmissionResult.ERR.getValue());
            submission.setErrorMessage(response != null ? response.getErr() : "Unknown error from judge server");
        }
        submissionRepository.save(submission);

        return response;
    }
}
