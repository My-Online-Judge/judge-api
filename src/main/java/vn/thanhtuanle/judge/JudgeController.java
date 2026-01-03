package vn.thanhtuanle.judge;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import vn.thanhtuanle.common.constant.Routes;
import vn.thanhtuanle.common.payload.ApiResponse;
import vn.thanhtuanle.judge.dto.JudgeResponseDto;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Routes.JUDGE)
@RequiredArgsConstructor
@Tag(name = "Judge Controller")
public class JudgeController {

    private final JudgeService judgeService;

    @Operation(summary = "Submit code", description = "Submit code to judge server")
    @PostMapping
    public ApiResponse<JudgeResponseDto> submitCode(@RequestBody JudgeSubmissionDto submissionDto) {
        return ApiResponse.success(judgeService.submitCode(submissionDto));
    }
}
