package vn.thanhtuanle.judge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeResultDto {
    @JsonProperty("cpu_time")
    private Integer cpuTime;

    @JsonProperty("real_time")
    private Integer realTime;

    private Long memory;

    private Integer signal;

    @JsonProperty("exit_code")
    private Integer exitCode;

    private Integer error;

    private Integer result;

    @JsonProperty("test_case")
    private String testCase;

    @JsonProperty("output_md5")
    private String outputMd5;

    private String output;
}
