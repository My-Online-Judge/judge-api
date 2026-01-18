package vn.thanhtuanle.language.dto;

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
public class LanguageResponseDto extends BaseResponse {

    private String name;

    private String identifier;

    private String extension;

    private String compileCommand;

    private String runCommand;

    private String seccompRule;

    private String srcName;

    private String exeName;

    private Long compileMaxMemory;

    private Long maxMemory;

    private String editorFormat;

    private boolean isDisabled = false;
}
