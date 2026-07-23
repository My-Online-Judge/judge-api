package vn.thanhtuanle.submission;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import vn.thanhtuanle.entity.Submission;
import vn.thanhtuanle.judge.dto.JudgeResultDto;
import vn.thanhtuanle.problem.TestCaseService;
import vn.thanhtuanle.problem.dto.TestCaseContext;
import vn.thanhtuanle.submission.dto.TestCaseResultDto;

import java.util.List;
import java.util.Map;

/**
 * The ONLY place a persisted {@code Submission.details} array becomes a user-facing DTO.
 *
 * <p>The stored array is the raw judge payload and contains the contestant's stdout for every
 * test case, including hidden ones. Exposing it verbatim would let a user submit a program that
 * echoes stdin and read back the hidden test data. Everything therefore flows through
 * {@link #assemble}, which drops that data unless the case is explicitly marked sample.
 *
 * <p>Do not read {@code Submission.getDetails()} anywhere else.
 */
@Component
@RequiredArgsConstructor
public class SubmissionDetailAssembler {

    private final TestCaseService testCaseService;

    /**
     * @return one row per judged test case, or {@code null} when the submission was judged
     *         before per-test details were persisted (distinct from an empty list, which means
     *         "judged, but no cases ran" — e.g. a compile error).
     */
    public List<TestCaseResultDto> assemble(Submission submission) {
        List<JudgeResultDto> details = submission.getDetails();
        if (details == null) {
            return null;
        }
        Map<String, TestCaseContext> byName = testCaseService.contextByName(submission.getProblem());
        return details.stream().map(d -> toRow(d, byName)).toList();
    }

    private TestCaseResultDto toRow(JudgeResultDto d, Map<String, TestCaseContext> byName) {
        // Fail closed: a name with no matching test case row (the admin edited test cases after
        // this submission was judged) is treated as hidden, never as sample.
        TestCaseContext ctx = byName.getOrDefault(d.getTestCase(), TestCaseContext.hidden());

        TestCaseResultDto.TestCaseResultDtoBuilder row = TestCaseResultDto.builder()
                .name(d.getTestCase())
                .result(d.getResult())
                .cpuTime(d.getCpuTime())
                .realTime(d.getRealTime())
                .memory(d.getMemory())
                .sample(ctx.sample());

        if (ctx.sample()) {
            row.input(ctx.input())
               .expectedOutput(ctx.expectedOutput())
               .actualOutput(d.getOutput());
        }
        // Hidden: input/expectedOutput/actualOutput are left unset. `output_md5`, `signal`,
        // `exit_code` and `error` are never exposed to users at all.
        return row.build();
    }
}
