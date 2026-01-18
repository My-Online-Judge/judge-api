package vn.thanhtuanle.submission.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import vn.thanhtuanle.common.payload.BaseResponse;
import vn.thanhtuanle.entity.Language;
import vn.thanhtuanle.judge.dto.JudgeResultDto;
import vn.thanhtuanle.language.dto.LanguageResponseDto;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SubmissionResponseDto extends BaseResponse {
    private String sourceCode;
    private Integer status;
    private Integer result;
    private String errorMessage;
    private Integer cpuTime;
    private Long memory;
    private Boolean shareSubmission;
    private LanguageResponseDto language;
    private List<JudgeResultDto> details;
}
