package vn.thanhtuanle.problem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.thanhtuanle.entity.Problem;
import vn.thanhtuanle.problem.dto.TestCaseContext;
import vn.thanhtuanle.submission.problem.JudgeSpec;
import vn.thanhtuanle.submission.problem.ProblemNotFoundException;
import vn.thanhtuanle.submission.problem.SampleTestCase;
import vn.thanhtuanle.testcase.TestCaseBundleStore;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalProblemCatalogTest {

    @Mock ProblemRepository problemRepository;
    @Mock TestCaseService testCaseService;
    @Mock TestCaseBundleStore bundleStore;
    @InjectMocks LocalProblemCatalog catalog;

    @Test
    void theJudgeSpecCarriesTheLimitsAndTheCurrentBundleVersion() {
        Problem problem = Problem.builder().problemSlug("a-plus-b").timeLimit(1000).memoryLimit(256L).build();
        problem.setId(UUID.randomUUID());
        when(problemRepository.findLiveBySlug("a-plus-b")).thenReturn(Optional.of(problem));
        when(bundleStore.currentVersion("a-plus-b")).thenReturn("abc123def456");

        assertThat(catalog.judgeSpec("a-plus-b"))
                .isEqualTo(new JudgeSpec(problem.getId(), "a-plus-b", 1000, 256L, "abc123def456"));
    }

    @Test
    void anUnknownOrDeletedProblemIsNotFound() {
        when(problemRepository.findLiveBySlug("gone")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalog.judgeSpec("gone")).isInstanceOf(ProblemNotFoundException.class)
                .hasMessage("Problem not found");
        verifyNoInteractions(bundleStore);
    }

    @Test
    void onlySampleTestCasesAreReturned() {
        UUID id = UUID.randomUUID();
        when(testCaseService.contextByProblemId(id)).thenReturn(Map.of(
                "1", new TestCaseContext(true, "1 2", "3"),
                "2", TestCaseContext.hidden()));

        assertThat(catalog.sampleCases(id)).containsExactly(Map.entry("1", new SampleTestCase("1", "1 2", "3")));
    }

    @Test
    void aFailureFailsClosedToNoSamples() {
        UUID id = UUID.randomUUID();
        when(testCaseService.contextByProblemId(id)).thenThrow(new IllegalStateException("database down"));

        assertThat(catalog.sampleCases(id)).isEmpty();
    }
}
