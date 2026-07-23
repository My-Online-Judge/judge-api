package vn.thanhtuanle.submission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One test case's outcome as shown to the submitting user. {@code input}, {@code expectedOutput}
 * and {@code actualOutput} are populated only when {@code sample} is true; for hidden cases they
 * stay null so no test data escapes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseResultDto {
    private String name;
    private Integer result;
    private Integer cpuTime;
    private Integer realTime;
    private Long memory;
    private boolean sample;
    private String input;
    private String expectedOutput;
    private String actualOutput;
}
