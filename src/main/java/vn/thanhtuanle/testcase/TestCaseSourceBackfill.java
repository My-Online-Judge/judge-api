package vn.thanhtuanle.testcase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.thanhtuanle.entity.Problem;
import vn.thanhtuanle.entity.TestCase;
import vn.thanhtuanle.problem.ProblemRepository;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * One-time move of the test-case files into the {@link TestCaseSourceStore} (sub-project 2a).
 *
 * <p>Until 2a the files lived on the container's disk, so every container recreate lost the ones added
 * through the API and the next publish silently dropped them from the bundle. For every row whose file
 * is not stored yet, this takes it from the problem's CURRENT bundle (what is actually judged) or else
 * from the copy shipped in the jar, and uploads it. A row found in neither place is an <b>orphan</b>: it
 * is logged with its id and never deleted here — deleting it is an operator step of the 2a runbook.
 * For every problem without orphans it then logs whether a publish would reproduce CURRENT.
 *
 * <p>Idempotent: on a restart it only re-checks. Removed in 2b together with the copies in the jar.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class TestCaseSourceBackfill implements ApplicationRunner {

    /** Where the jar carries the files committed with the first problems. */
    static final String SHIPPED_COPIES = "test_cases/";

    private final ProblemRepository problemRepository;
    private final TestCaseSourceStore sources;
    private final TestCaseBundleStore bundles;
    private final TestCaseBundlePublisher publisher;

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        bundles.ensureBucket();
        Report report = new Report();
        for (Problem problem : problemRepository.findAll()) {
            try {
                backfill(problem, report);
            } catch (RuntimeException e) {
                report.failed.add(problem.getProblemSlug());
                log.error("Test-case backfill aborted for {}: {}", problem.getProblemSlug(), e.getMessage(), e);
            }
        }
        log.info("Test-case backfill done: uploaded={} orphans={} mismatched={} failed={}",
                report.uploaded, report.orphans, report.mismatched, report.failed);
    }

    private void backfill(Problem problem, Report report) {
        String slug = problem.getProblemSlug();
        Map<String, byte[]> current = bundles.currentBundleFiles(slug);
        boolean hasOrphans = false;
        for (TestCase tc : problem.getTestCases()) {
            boolean missing = false;
            for (String path : List.of(tc.getInput(), tc.getOutput())) {
                if (sources.exists(path)) {
                    continue;
                }
                Optional<byte[]> content = Optional
                        .ofNullable(current.get(TestCaseBundlePublisher.fileName(path)))
                        .or(() -> shippedCopy(path));
                if (content.isPresent()) {
                    sources.put(path, content.get());
                    report.uploaded++;
                } else {
                    missing = true;
                }
            }
            if (missing) {
                hasOrphans = true;
                report.orphans.add(tc.getId());
                log.error("ORPHAN test case id={} problem={} input={}: its files are neither in the CURRENT bundle "
                        + "nor in the jar", tc.getId(), slug, tc.getInput());
            }
        }
        if (hasOrphans) {
            return;
        }
        if (current.isEmpty()) {
            log.info("Backfill: {} had no bundle; published {}", slug, publisher.publish(problem));
            return;
        }
        String wouldPublish = publisher.bundleHash(problem);
        String currentHash = bundles.currentVersion(slug);
        boolean match = wouldPublish.equals(currentHash);
        if (!match) {
            report.mismatched.add(slug);
        }
        log.info("Backfill: {} publish-hash={} CURRENT={} match={}", slug, wouldPublish, currentHash, match);
    }

    private Optional<byte[]> shippedCopy(String path) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(SHIPPED_COPIES + path)) {
            return in == null ? Optional.empty() : Optional.of(in.readAllBytes());
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static final class Report {
        int uploaded;
        final List<UUID> orphans = new ArrayList<>();
        final List<String> mismatched = new ArrayList<>();
        final List<String> failed = new ArrayList<>();
    }
}
