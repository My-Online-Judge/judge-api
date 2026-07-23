package vn.thanhtuanle.problem;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import vn.thanhtuanle.common.constant.AppProperties;
import vn.thanhtuanle.common.constant.Routes;
import vn.thanhtuanle.common.payload.ApiResponse;
import vn.thanhtuanle.problem.dto.ProblemResponseDto;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Problem Package Controller")
public class ProblemPackageController {

    private final ProblemPackageService problemPackageService;

    @PostMapping(value = Routes.PROBLEMS + "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Import a problem from a package zip (admin only)")
    @PreAuthorize("hasAuthority('problem:create')")
    public ApiResponse<ProblemResponseDto> importProblem(
            @RequestPart(AppProperties.REQUEST_PART_FILE) MultipartFile file,
            @RequestParam(required = false) String slugOverride) throws IOException {
        log.info("Start import problem package (slugOverride={})", slugOverride);
        ProblemResponseDto problem = problemPackageService.importProblem(file, slugOverride);
        log.info("End import problem package: ID={}", problem.getId());
        return ApiResponse.created(problem);
    }

    @GetMapping(Routes.PROBLEMS + "/{slug}/export")
    @Operation(summary = "Export a problem as a package zip (admin only)")
    @PreAuthorize("hasAuthority('problem:update')")
    public ResponseEntity<byte[]> exportProblem(@PathVariable String slug) throws IOException {
        log.info("Start export problem package: {}", slug);
        byte[] zip = problemPackageService.exportProblem(slug);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + slug + ".zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zip);
    }
}
