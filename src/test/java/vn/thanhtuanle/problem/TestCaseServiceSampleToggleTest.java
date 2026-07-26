package vn.thanhtuanle.problem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.thanhtuanle.common.exception.ResourceNotFoundException;
import vn.thanhtuanle.common.util.GenerateTestCaseInfoUtil;
import vn.thanhtuanle.entity.Problem;
import vn.thanhtuanle.entity.TestCase;
import vn.thanhtuanle.problem.dto.TestCaseResponse;
import vn.thanhtuanle.testcase.TestCaseBundleStore;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestCaseServiceSampleToggleTest {

    @Mock ProblemRepository problemRepository;
    @Mock TestCaseRepository testCaseRepository;
    @Mock GenerateTestCaseInfoUtil infoGenerator;
    @Mock TestCaseBundleStore bundleStore;
    @InjectMocks TestCaseService service;

    @Test
    void setsFlag_withoutRepublishingBundle() {
        UUID id = UUID.randomUUID();
        Problem problem = new Problem();
        problem.setProblemSlug("a-plus-b");
        TestCase tc = TestCase.builder().input("a-plus-b/1.in").output("a-plus-b/1.out")
                .problem(problem).build();
        tc.setId(id);
        when(testCaseRepository.findById(id)).thenReturn(Optional.of(tc));

        TestCaseResponse response = service.setSample("a-plus-b", id, true);

        assertThat(tc.isSample()).isTrue();
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.isSample()).isTrue();
        // The bundle hash must not change: no re-judge churn from a visibility toggle.
        verifyNoInteractions(bundleStore);
        verifyNoInteractions(infoGenerator);
    }

    @Test
    void slugMismatch_throwsAndDoesNotSave() {
        UUID id = UUID.randomUUID();
        Problem problem = new Problem();
        problem.setProblemSlug("a-plus-b");
        TestCase tc = TestCase.builder().input("a-plus-b/1.in").output("a-plus-b/1.out")
                .problem(problem).build();
        tc.setId(id);
        when(testCaseRepository.findById(id)).thenReturn(Optional.of(tc));

        assertThatThrownBy(() -> service.setSample("other-slug", id, true))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(testCaseRepository, never()).save(any());
        verifyNoInteractions(bundleStore);
        verifyNoInteractions(infoGenerator);
    }

    @Test
    void nullProblem_throwsAndDoesNotSave() {
        UUID id = UUID.randomUUID();
        TestCase tc = TestCase.builder().input("a-plus-b/1.in").output("a-plus-b/1.out").build();
        tc.setId(id);
        when(testCaseRepository.findById(id)).thenReturn(Optional.of(tc));

        assertThatThrownBy(() -> service.setSample("a-plus-b", id, true))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(testCaseRepository, never()).save(any());
        verifyNoInteractions(bundleStore);
        verifyNoInteractions(infoGenerator);
    }
}
