package vn.thanhtuanle.judge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeLanguageConfigDto {
    private JudgeCompileConfigDto compile;
    private JudgeRunConfigDto run;
}
