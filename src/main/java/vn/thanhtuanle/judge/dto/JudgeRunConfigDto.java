package vn.thanhtuanle.judge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeRunConfigDto {
    private String command;

    @JsonProperty("seccomp_rule")
    private String seccompRule;

    private List<String> env;

    /**
     * 1 = judge_server measures memory but does NOT enforce it via rlimit (it still flags MLE
     * by comparing actual usage to max_memory). Required for the JVM, whose huge virtual-memory
     * reservation trips a hard rlimit before user code even runs. 0 = hard rlimit enforcement.
     */
    @JsonProperty("memory_limit_check_only")
    private Integer memoryLimitCheckOnly;
}
