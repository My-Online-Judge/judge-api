package vn.thanhtuanle.submission;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "Submit a solution for a problem")
    public ApiResponse<SubmissionResponseDto> submit(@Valid @RequestBody SubmissionRequestDto submissionDto) {
        log.info("Received submission request for problem: {}", submissionDto.getProblemSlug());
        return ApiResponse.success(submissionService.submit(submissionDto));
    }

    @GetMapping
    @Operation(summary = "Get submissions by problem slug")
    public ApiResponse<List<SubmissionResponseDto>> getSubmissionsByProblemSlug(
            @RequestParam(name = "problem") String problemSlug) {
        log.info("Fetching submissions for problem slug: {}", problemSlug);
        return ApiResponse.success(submissionService.getSubmissionsByProblemSlug(problemSlug));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get submissions by user ID")
    public ApiResponse<List<SubmissionResponseDto>> getSubmissionsByUserId(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching submissions for user ID: {}", userId);
        return ApiResponse.success(submissionService.getSubmissionsByUser(userId, page, size));
    }

    @GetMapping("/user/{userId}/problem/{problemSlug}")
    @Operation(summary = "Get submissions by user ID and problem slug")
    public ApiResponse<List<SubmissionResponseDto>> getSubmissionsByUserAndProblem(
            @PathVariable String userId,
            @PathVariable String problemSlug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching submissions for user ID: {} and problem slug: {}", userId, problemSlug);
        return ApiResponse.success(submissionService.getSubmissionsByUserAndProblem(userId, problemSlug, page, size));
    }
}
