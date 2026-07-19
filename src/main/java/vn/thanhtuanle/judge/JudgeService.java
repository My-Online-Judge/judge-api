package vn.thanhtuanle.judge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import vn.thanhtuanle.common.constant.AppProperties;
import vn.thanhtuanle.entity.Language;
import vn.thanhtuanle.entity.Problem;
import vn.thanhtuanle.judge.dto.*;
import vn.thanhtuanle.messaging.event.SubmissionRequestedEvent;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JudgeService {

    private final vn.thanhtuanle.testcase.TestCaseBundleStore bundleStore;

    private static final int MAX_CODE_LENGTH = 10240;
    private static final long BYTES_PER_MB = 1024L * 1024L;

    /**
     * Languages whose runtime reserves a large virtual-memory space (so a hard memory rlimit
     * would kill them before user code runs). For these, judge_server only *checks* memory
     * usage instead of enforcing it via rlimit. Mirrors QingdaoU's per-language config.
     */
    private static final List<String> MEMORY_CHECK_ONLY_LANGUAGES = List.of("java");

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

    public SubmissionRequestedEvent buildRequestedEvent(String submissionId, String sourceCode,
            Problem problem, Language language) {
        log.info("Building judge request for problem: {}", problem.getProblemSlug());

        JudgeCompileConfigDto compileConfig = JudgeCompileConfigDto.builder()
                .srcName(language.getSrcName())
                .exeName(language.getExeName())
                .maxCpuTime(3000)
                .maxRealTime(5000)
                .maxMemory(language.getCompileMaxMemory())
                .compileCommand(language.getCompileCommand())
                .build();

        int memoryCheckOnly = MEMORY_CHECK_ONLY_LANGUAGES.contains(language.getIdentifier()) ? 1 : 0;
        JudgeRunConfigDto runConfig = JudgeRunConfigDto.builder()
                .command(language.getRunCommand())
                .seccompRule(language.getSeccompRule())
                .env(AppProperties.JUDGE_ENV)
                .memoryLimitCheckOnly(memoryCheckOnly)
                .build();

        JudgeLanguageConfigDto languageConfig = JudgeLanguageConfigDto.builder()
                .compile(compileConfig)
                .run(runConfig)
                .build();

        return SubmissionRequestedEvent.builder()
                .submissionId(submissionId)
                .src(sourceCode)
                .languageConfig(languageConfig)
                .maxCpuTime(problem.getTimeLimit())
                .maxMemory(problem.getMemoryLimit() * BYTES_PER_MB)
                .testCaseId(problem.getProblemSlug() + "__" + bundleStore.currentVersion(problem.getProblemSlug()))
                .output(true)
                .build();
    }
}
