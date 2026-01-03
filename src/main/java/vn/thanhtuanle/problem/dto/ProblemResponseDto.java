package vn.thanhtuanle.problem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import vn.thanhtuanle.common.payload.BaseResponse;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ProblemResponseDto extends BaseResponse {
    private String title;
    private String description;
    private int timeLimit;
    private int memoryLimit;
    private int hardnessLevel;
    private String problemSlug;
    private String sampleInput;
    private String sampleOutput;
    private String inputDescription;
    private String outputDescription;
    private String hint;
    private int status;
}
