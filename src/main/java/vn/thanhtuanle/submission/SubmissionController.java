package vn.thanhtuanle.submission;

import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import vn.thanhtuanle.common.constant.Routes;
import vn.thanhtuanle.common.payload.ApiResponse;
import vn.thanhtuanle.submission.dto.SubmissionRequestDto;
import vn.thanhtuanle.submission.dto.SubmissionResponseDto;

@RestController
@RequestMapping(Routes.SUBMISSIONS)
@RequiredArgsConstructor
@Slf4j
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    public ApiResponse<SubmissionResponseDto> submit(@Valid @RequestBody SubmissionRequestDto submissionDto) {
        log.info("Received submission request for problem: {}", submissionDto.getProblemSlug());
        return ApiResponse.success(submissionService.submit(submissionDto));
    }

    @GetMapping
    public ApiResponse<List<SubmissionResponseDto>> getSubmissionsByProblemSlug(@RequestParam(name = "problem") String problemSlug) {
        log.info("Fetching submissions for problem slug: {}", problemSlug);
        return ApiResponse.success(submissionService.getSubmissionsByProblemSlug(problemSlug));
    }
}
