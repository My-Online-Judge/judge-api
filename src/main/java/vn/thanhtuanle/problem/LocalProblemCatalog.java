package vn.thanhtuanle.problem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.thanhtuanle.entity.Problem;
import vn.thanhtuanle.submission.problem.JudgeSpec;
import vn.thanhtuanle.submission.problem.ProblemCatalog;
import vn.thanhtuanle.submission.problem.ProblemNotFoundException;
import vn.thanhtuanle.submission.problem.SampleTestCase;
import vn.thanhtuanle.testcase.TestCaseBundleStore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** {@link ProblemCatalog} over this service's own problem tables. Replaced by a gRPC client in 2b. */
@Component
@RequiredArgsConstructor
@Slf4j
public class LocalProblemCatalog implements ProblemCatalog {

    private final ProblemRepository problemRepository;
    private final TestCaseService testCaseService;
    private final TestCaseBundleStore bundleStore;

    @Override
    @Transactional(readOnly = true)
    public JudgeSpec judgeSpec(String slug) {
        Problem problem = problemRepository.findLiveBySlug(slug).orElseThrow(ProblemNotFoundException::new);
        return new JudgeSpec(problem.getId(), problem.getProblemSlug(), problem.getTimeLimit(),
                problem.getMemoryLimit(), bundleStore.currentVersion(problem.getProblemSlug()));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, SampleTestCase> sampleCases(UUID problemId) {
        try {
            Map<String, SampleTestCase> samples = new HashMap<>();
            testCaseService.contextByProblemId(problemId).forEach((name, ctx) -> {
                if (ctx.sample()) {
                    samples.put(name, new SampleTestCase(name, ctx.input(), ctx.expectedOutput()));
                }
            });
            return samples;
        } catch (RuntimeException e) {
            log.warn("Sample test cases of problem {} unavailable, every row shows as hidden: {}", problemId, e.getMessage());
            return Map.of();
        }
    }
}
