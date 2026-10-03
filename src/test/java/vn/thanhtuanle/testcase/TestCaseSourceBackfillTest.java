package vn.thanhtuanle.testcase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.thanhtuanle.entity.Problem;
import vn.thanhtuanle.entity.TestCase;
import vn.thanhtuanle.problem.ProblemRepository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestCaseSourceBackfillTest {

    @Mock ProblemRepository problemRepository;
    @Mock TestCaseBundleStore bundles;
    @Mock TestCaseBundlePublisher publisher;
    private final InMemoryTestCaseSourceStore sources = new InMemoryTestCaseSourceStore();
    private TestCaseSourceBackfill backfill;

    @BeforeEach
    void setUp() {
        backfill = new TestCaseSourceBackfill(problemRepository, sources, bundles, publisher);
    }

    private static Problem problemWith(String slug, int... indices) {
        Problem problem = Problem.builder().problemSlug(slug).testCases(new ArrayList<>()).build();
        for (int i : indices) {
            TestCase tc = TestCase.builder()
                    .input(slug + "/" + i + ".in").output(slug + "/" + i + ".out").problem(problem).build();
            tc.setId(UUID.randomUUID());
            problem.getTestCases().add(tc);
        }
        return problem;
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void aFileNotStoredYetIsTakenFromTheCurrentBundle() {
        Problem p = problemWith("p", 1);
        when(problemRepository.findAll()).thenReturn(List.of(p));
        when(bundles.currentBundleFiles("p")).thenReturn(Map.of("1.in", bytes("1 2\n"), "1.out", bytes("3\n"), "info", bytes("{}")));
        when(publisher.bundleHash(p)).thenReturn("aaaaaaaaaaaa");
        when(bundles.currentVersion("p")).thenReturn("aaaaaaaaaaaa");

        backfill.run(null);

        assertThat(sources.files).containsOnlyKeys("p/1.in", "p/1.out");
        assertThat(new String(sources.files.get("p/1.out"), StandardCharsets.UTF_8)).isEqualTo("3\n");
        verify(publisher, never()).publish(any());   // CURRENT already matches: nothing to republish
    }

    @Test
    void aRowFoundNowhereIsReportedAndKeptAndNothingIsPublished() {
        // Live case: simple-a-plus-b has rows 1–7 but its CURRENT bundle holds only 1, 5, 6, 7.
        Problem p = problemWith("p", 1, 2);
        when(problemRepository.findAll()).thenReturn(List.of(p));
        when(bundles.currentBundleFiles("p")).thenReturn(Map.of("1.in", bytes("1 2\n"), "1.out", bytes("3\n")));

        backfill.run(null);

        assertThat(sources.files).containsOnlyKeys("p/1.in", "p/1.out");
        assertThat(p.getTestCases()).hasSize(2);
        verify(publisher, never()).publish(any());
        verify(publisher, never()).bundleHash(any());
        verify(problemRepository, never()).save(any());
    }

    @Test
    void theJarCopyIsUsedWhenTheProblemHasNoBundleAndThenItIsPublished() throws IOException {
        // src/main/resources/test_cases/simple-a-plus-b/1.in|out ship in the jar.
        Problem p = problemWith("simple-a-plus-b", 1);
        when(problemRepository.findAll()).thenReturn(List.of(p));
        when(bundles.currentBundleFiles("simple-a-plus-b")).thenReturn(Map.of());

        backfill.run(null);

        assertThat(sources.files.get("simple-a-plus-b/1.in")).isEqualTo(shipped("simple-a-plus-b/1.in"));
        verify(publisher).publish(p);
    }

    @Test
    void storedFilesAreLeftAlone() {
        Problem p = problemWith("p", 1);
        sources.put("p/1.in", bytes("stored in"));
        sources.put("p/1.out", bytes("stored out"));
        when(problemRepository.findAll()).thenReturn(List.of(p));
        when(bundles.currentBundleFiles("p")).thenReturn(Map.of("1.in", bytes("other"), "1.out", bytes("other")));
        when(publisher.bundleHash(p)).thenReturn("aaaaaaaaaaaa");
        when(bundles.currentVersion("p")).thenReturn("bbbbbbbbbbbb");

        backfill.run(null);

        assertThat(new String(sources.files.get("p/1.in"), StandardCharsets.UTF_8)).isEqualTo("stored in");
        verify(publisher, never()).publish(any());   // a mismatch is reported, never "repaired"
    }

    @Test
    void oneBrokenProblemDoesNotStopTheOthers() {
        Problem bad = problemWith("bad", 1);
        Problem good = problemWith("good", 1);
        when(problemRepository.findAll()).thenReturn(List.of(bad, good));
        when(bundles.currentBundleFiles("bad")).thenThrow(new TestCaseBundleException("MinIO said 403"));
        when(bundles.currentBundleFiles("good")).thenReturn(Map.of("1.in", bytes("x"), "1.out", bytes("y")));
        when(publisher.bundleHash(good)).thenReturn("aaaaaaaaaaaa");
        when(bundles.currentVersion("good")).thenReturn("aaaaaaaaaaaa");

        backfill.run(null);

        assertThat(sources.files).containsOnlyKeys("good/1.in", "good/1.out");
    }

    private static byte[] shipped(String path) throws IOException {
        try (InputStream in = TestCaseSourceBackfillTest.class.getClassLoader()
                .getResourceAsStream("test_cases/" + path)) {
            return in.readAllBytes();
        }
    }
}
