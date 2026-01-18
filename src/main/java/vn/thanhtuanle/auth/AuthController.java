package vn.thanhtuanle.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import vn.thanhtuanle.auth.dto.AuthResponse;
import vn.thanhtuanle.auth.dto.ExchangeTokenRequest;
import vn.thanhtuanle.auth.dto.IntrospectRequest;
import vn.thanhtuanle.auth.dto.IntrospectResponse;
import vn.thanhtuanle.common.constant.Routes;
import vn.thanhtuanle.common.payload.ApiResponse;
import vn.thanhtuanle.user.dto.UserResponse;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
@RequestMapping(Routes.AUTH)
@RequiredArgsConstructor
@Tag(name = "Auth Controller")
@Slf4j
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Get Google Auth URL", description = "Get URL to redirect user to Google Login")
    @GetMapping("/outbound/google")
    public ApiResponse<Map<String, String>> getGoogleAuthUrl() {
        return ApiResponse.success(Map.of("url", authService.getGoogleAuthUrl()));
    }

    @Operation(summary = "Logout", description = "Logout user and clear cookies")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletResponse response) {
        authService.logout();

        Cookie accessTokenCookie = new Cookie("accessToken", null);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(false);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(0);
        response.addCookie(accessTokenCookie);

        Cookie refreshTokenCookie = new Cookie("refreshToken", null);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(0);
        response.addCookie(refreshTokenCookie);

        return ApiResponse.success();
    }

    @Operation(summary = "Introspect Token", description = "Check if token is valid")
    @PostMapping("/introspect")
    public ApiResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request) {
        return ApiResponse.success(authService.introspect(request));
    }

    @Operation(summary = "Login with Google", description = "Exchange Google code for Access Token")
    @PostMapping("/outbound/authentication")
    public ApiResponse<AuthResponse> loginGoogle(@Valid @RequestBody ExchangeTokenRequest request,
            HttpServletResponse response) {
        var authResponse = authService.authenticateGoogleUser(request.getCode());

        // Set Access Token Cookie
        Cookie accessTokenCookie = new Cookie("accessToken", authResponse.getAccessToken());
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(false);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(24 * 60 * 60); // 1 day
        response.addCookie(accessTokenCookie);

        // Set Refresh Token Cookie
        Cookie refreshTokenCookie = new Cookie("refreshToken", authResponse.getRefreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        response.addCookie(refreshTokenCookie);

        return ApiResponse.success(authResponse);
    }

    @Operation(summary = "Me", description = "Get current user info")
    @GetMapping("/me")
    public ApiResponse<UserResponse> getMe() {
        log.info("Me request received");
        return ApiResponse.success(authService.me());
    }
}
