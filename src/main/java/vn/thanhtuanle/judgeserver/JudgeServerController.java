package vn.thanhtuanle.judgeserver;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import vn.thanhtuanle.common.constant.AppProperties;
import vn.thanhtuanle.common.constant.Routes;
import vn.thanhtuanle.common.payload.ApiResponse;
import vn.thanhtuanle.judgeserver.dto.JudgeServerHeartbeatDto;
import vn.thanhtuanle.judgeserver.dto.JudgeServerResponseDto;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Judge Server Controller")
public class JudgeServerController {

    private final JudgeServerService judgeServerService;

    /**
     * Receives periodic heartbeats from judge_server instances. Authenticated by the
     * X-Judge-Server-Token header (sha256 of the shared token), NOT by JWT — so it is
     * whitelisted in SecurityConfig. Both the slashed and unslashed paths are mapped because
     * the judge_server's BACKEND_URL ends with a trailing slash and Spring Boot 3 does not
     * match it implicitly.
     */
    @Operation(summary = "Judge server heartbeat (called by judge_server, not the UI)")
    @PostMapping({ Routes.JUDGE_SERVER_HEARTBEAT, Routes.JUDGE_SERVER_HEARTBEAT + "/" })
    public ResponseEntity<ApiResponse<Void>> heartbeat(
            @RequestBody JudgeServerHeartbeatDto dto,
            @RequestHeader(value = AppProperties.X_JUDGE_SERVER_TOKEN, required = false) String token,
            HttpServletRequest request) {

        boolean accepted = judgeServerService.handleHeartbeat(dto, token, resolveClientIp(request));
        if (!accepted) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<Void>builder()
                            .status(HttpStatus.UNAUTHORIZED.value())
                            .message("Invalid judge server token")
                            .build());
        }
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "List registered judge servers and their health (admin only)")
    @GetMapping(Routes.JUDGE_SERVERS)
    @PreAuthorize("hasAuthority('ADMIN')")
    public ApiResponse<List<JudgeServerResponseDto>> listJudgeServers() {
        return ApiResponse.success(judgeServerService.listServers());
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
