package vn.thanhtuanle.problem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Wire format of problem.json inside an import/export package (schemaVersion 1). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProblemPackageDto {
    private int schemaVersion;
    private CreateProblemDto problem;
}
