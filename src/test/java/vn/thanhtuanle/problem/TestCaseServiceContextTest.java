package vn.thanhtuanle.problem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.thanhtuanle.common.util.GenerateTestCaseInfoUtil;
import vn.thanhtuanle.entity.Problem;
import vn.thanhtuanle.entity.TestCase;
import vn.thanhtuanle.problem.dto.TestCaseContext;
import vn.thanhtuanle.testcase.TestCaseBundleStore;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TestCaseServiceContextTest {

    @Mock ProblemRepository problemRepository;
    @Mock TestCaseRepository testCaseRepository;
    @Mock GenerateTestCaseInfoUtil infoGenerator;
    @Mock TestCaseBundleStore bundleStore;
    @InjectMocks TestCaseService service;

    private TestCase tc(String slugPath, boolean sample) {
        TestCase t = TestCase.builder()
                .input(slugPath + ".in")
                .output(slugPath + ".out")
                .build();
        t.setSample(sample);
        return t;
    }

    @Test
    void keysByNumericBaseName_andFlagsVisibility() {
        Problem p = new Problem();
        p.setTestCases(List.of(tc("a-plus-b/1", true), tc("a-plus-b/2", false)));

        Map<String, TestCaseContext> ctx = service.contextByName(p);

        assertThat(ctx).containsOnlyKeys("1", "2");
        assertThat(ctx.get("1").sample()).isTrue();
        assertThat(ctx.get("2").sample()).isFalse();
    }

    @Test
    void hiddenCaseCarriesNoContent() {
        Problem p = new Problem();
        p.setTestCases(List.of(tc("a-plus-b/2", false)));

        TestCaseContext hidden = service.contextByName(p).get("2");

        assertThat(hidden.input()).isNull();
        assertThat(hidden.expectedOutput()).isNull();
    }
}
