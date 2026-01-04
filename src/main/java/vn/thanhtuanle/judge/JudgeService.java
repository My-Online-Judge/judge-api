package vn.thanhtuanle.judge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import vn.thanhtuanle.common.constant.AppProperties;
import vn.thanhtuanle.entity.Language;
import vn.thanhtuanle.entity.Problem;
import vn.thanhtuanle.judge.dto.*;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JudgeService {

    @Value("${judge.server.url}")
    private String judgeServerUrl;

    @Value("${judge.server.token}")
    private String judgeServerToken;

    private final RestClient.Builder restClientBuilder;

    private static final int MAX_CODE_LENGTH = 10240;
    private static final List<String> BLACKLISTED_KEYWORDS = List.of(
            "Runtime.getRuntime().exec",
            "/bin/sh",
            "ProcessBuilder",
            "java.lang.Runtime");

    public void validate(String sourceCode) {
        log.info("Start validate source code");
        if (sourceCode == null || sourceCode.isEmpty()) {
            throw new IllegalArgumentException("Source code cannot be empty");
        }
        if (sourceCode.length() > MAX_CODE_LENGTH) {
            throw new IllegalArgumentException("Source code length exceeds the limit");
        }
        for (String keyword : BLACKLISTED_KEYWORDS) {
            if (sourceCode.contains(keyword)) {
                throw new IllegalArgumentException("Source code contains blacklisted keyword: " + keyword);
            }
        }
        log.info("End validate source code");
    }

    public JudgeResponseDto judge(String sourceCode, Problem problem, Language language) {
        log.info("Start judge source code for problem: {}", problem.getProblemSlug());
        JudgeCompileConfigDto compileConfig = JudgeCompileConfigDto.builder()
                .srcName(language.getSrcName())
                .exeName(language.getExeName())
                .maxCpuTime(3000)
                .maxRealTime(5000)
                .maxMemory(128L * 1024 * 1024)
                .compileCommand(language.getCompileCommand())
                .build();

        JudgeRunConfigDto runConfig = JudgeRunConfigDto.builder()
                .command(language.getRunCommand())
                .seccompRule(language.getSeccompRule())
                .env(AppProperties.JUDGE_ENV)
                .build();

        JudgeLanguageConfigDto languageConfig = JudgeLanguageConfigDto.builder()
                .compile(compileConfig)
                .run(runConfig)
                .build();

        JudgeSubmissionDto request = JudgeSubmissionDto.builder()
                .src(sourceCode)
                .languageConfig(languageConfig)
                .maxCpuTime(problem.getTimeLimit())
                .maxMemory((long) problem.getMemoryLimit() * 1024 * 1024)
                .testCaseId(problem.getProblemSlug())
                .output(true)
                .build();

        log.info("Calling judge server at: {}", judgeServerUrl);
        return restClientBuilder.build()
                .post()
                .uri(String.format("%s%s", judgeServerUrl, AppProperties.JUDGE_SERVER_ENDPOINT))
                .header(AppProperties.X_JUDGE_SERVER_TOKEN, judgeServerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(JudgeResponseDto.class);
    }
}
