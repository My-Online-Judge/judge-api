package vn.thanhtuanle.testcase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import vn.thanhtuanle.entity.Problem;
import vn.thanhtuanle.problem.ProblemRepository;

/**
 * On boot, ensures every existing problem has a test-case bundle in MinIO. Idempotent: a problem
 * that already has a CURRENT pointer is not republished, but is still pruned so pre-existing
 * duplicate versions get cleaned up on boot. Excluded from the {@code test} profile so the H2
 * context test never touches a real MinIO.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class TestCaseBundleBootstrap implements ApplicationRunner {

    private final ProblemRepository problemRepository;
    private final TestCaseBundleStore store;

    @Override
    public void run(ApplicationArguments args) {
        store.ensureBucket();
        for (Problem problem : problemRepository.findAll()) {
            String slug = problem.getProblemSlug();
            if (slug == null) {
                continue;
            }
            try {
                if (!store.hasBundle(slug)) {
                    store.publish(slug);
                    log.info("Seeded MinIO test-case bundle for {}", slug);
                }
                store.pruneOldBundles(slug, store.currentVersion(slug));
            } catch (Exception e) {
                log.warn("Could not seed test-case bundle for {}: {}", slug, e.getMessage());
            }
        }
    }
}
