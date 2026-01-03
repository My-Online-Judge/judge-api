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
}
