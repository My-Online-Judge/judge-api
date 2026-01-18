package vn.thanhtuanle.language;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.thanhtuanle.common.constant.Routes;
import vn.thanhtuanle.common.enums.ProblemStatus;
import vn.thanhtuanle.common.payload.ApiResponse;
import vn.thanhtuanle.language.dto.LanguageResponseDto;

import java.util.List;

@RestController
@RequestMapping(Routes.LANGUAGES)
@RequiredArgsConstructor
@Slf4j
public class LanguageController {

    private final LanguageService languageService;

    @GetMapping
    @Operation(summary = "Get list of languages")
    public ApiResponse<List<LanguageResponseDto>> getLanguages() {
        log.info("Fetch all programming languages");
        return ApiResponse.success(languageService.getAllLanguages());
    }
}
