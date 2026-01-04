package vn.thanhtuanle.judge;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.thanhtuanle.judge.dto.JudgeLanguageConfigDto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeSubmissionDto {
    private String src;

    @JsonProperty("language_config")
    private JudgeLanguageConfigDto languageConfig;

    @JsonProperty("max_cpu_time")
    private Integer maxCpuTime;

    @JsonProperty("max_memory")
    private Long maxMemory;

    @JsonProperty("test_case_id")
    private String testCaseId;

    private Boolean output;
}
