package vn.thanhtuanle.submission;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.thanhtuanle.entity.Problem;
import vn.thanhtuanle.entity.Submission;
import vn.thanhtuanle.judge.dto.JudgeResultDto;
import vn.thanhtuanle.problem.TestCaseService;
import vn.thanhtuanle.problem.dto.TestCaseContext;
import vn.thanhtuanle.submission.dto.TestCaseResultDto;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionDetailAssemblerTest {

    @Mock TestCaseService testCaseService;
    @InjectMocks SubmissionDetailAssembler assembler;

    private Submission submissionWith(List<JudgeResultDto> details) {
        Problem p = new Problem();
        Submission s = Submission.builder().problem(p).build();
        s.setDetails(details);
        return s;
    }

    private JudgeResultDto judged(String name, int result) {
        return JudgeResultDto.builder()
                .testCase(name).result(result)
                .cpuTime(12).realTime(15).memory(3072L)
                .output("42").outputMd5("deadbeef")
                .build();
    }

    @Test
    void hiddenCase_neverExposesTestData() {
        Submission s = submissionWith(List.of(judged("2", -1)));
        when(testCaseService.contextByName(s.getProblem()))
                .thenReturn(Map.of("2", TestCaseContext.hidden()));

        TestCaseResultDto row = assembler.assemble(s).get(0);

        assertThat(row.isSample()).isFalse();
        assertThat(row.getInput()).isNull();
        assertThat(row.getExpectedOutput()).isNull();
        assertThat(row.getActualOutput()).isNull();
        // metrics are still exposed
        assertThat(row.getCpuTime()).isEqualTo(12);
        assertThat(row.getMemory()).isEqualTo(3072L);
        assertThat(row.getResult()).isEqualTo(-1);
    }

    @Test
    void sampleCase_exposesInputExpectedAndActual() {
        Submission s = submissionWith(List.of(judged("1", -1)));
        when(testCaseService.contextByName(s.getProblem()))
                .thenReturn(Map.of("1", new TestCaseContext(true, "3 4", "7")));

        TestCaseResultDto row = assembler.assemble(s).get(0);

        assertThat(row.isSample()).isTrue();
        assertThat(row.getInput()).isEqualTo("3 4");
        assertThat(row.getExpectedOutput()).isEqualTo("7");
        assertThat(row.getActualOutput()).isEqualTo("42");
    }

    @Test
    void unknownTestCaseName_failsClosedAsHidden() {
        // Admin edited test cases after this submission was judged.
        Submission s = submissionWith(List.of(judged("99", -1)));
        when(testCaseService.contextByName(s.getProblem())).thenReturn(Map.of());

        TestCaseResultDto row = assembler.assemble(s).get(0);

        assertThat(row.isSample()).isFalse();
        assertThat(row.getInput()).isNull();
        assertThat(row.getExpectedOutput()).isNull();
        assertThat(row.getActualOutput()).isNull();
    }

    @Test
    void nullDetails_returnsNull() {
        Submission s = submissionWith(null);

        assertThat(assembler.assemble(s)).isNull();
    }

    @Test
    void emptyDetails_returnsEmptyList() {
        Submission s = submissionWith(List.of());

        assertThat(assembler.assemble(s)).isEmpty();
    }
}
