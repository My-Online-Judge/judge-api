package vn.thanhtuanle.submission.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionRequestDto {

    private String sourceCode;

    private String languageIdentifier;

    private String problemSlug;
}
