package vn.thanhtuanle.problem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import vn.thanhtuanle.common.payload.ApiResponse;

import vn.thanhtuanle.common.constant.AppProperties;
import vn.thanhtuanle.common.constant.Routes;
import vn.thanhtuanle.common.enums.ProblemStatus;
import vn.thanhtuanle.problem.dto.CreateProblemDto;
import vn.thanhtuanle.problem.dto.ProblemResponseDto;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping(Routes.PROBLEMS)
@RequiredArgsConstructor
@Slf4j
public class ProblemController {

    private final ProblemService problemService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create a new problem")
    public ApiResponse<ProblemResponseDto> createProblem(
            @Valid @RequestPart(AppProperties.REQUEST_PART_DATA) CreateProblemDto dto,
            @RequestPart(AppProperties.REQUEST_PART_FILE) MultipartFile file) throws IOException {

        log.info("Start create problem: title={}, slug={}", dto.getTitle(), dto.getProblemSlug());
        ProblemResponseDto problem = problemService.createProblem(dto, file);
        log.info("END create problem: ID={}", problem.getId());
        return ApiResponse.created(problem);
    }

    @GetMapping
    @Operation(summary = "Get list of problems")
    public ApiResponse<List<ProblemResponseDto>> getProblems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ProblemStatus status) {

        log.info("Start get problems: page={}, size={}, search={}, status={}", page, size, search, status);

        log.info("END get problems");
        return ApiResponse.success(problemService.getProblems(page, size, search, status));
    }
}
