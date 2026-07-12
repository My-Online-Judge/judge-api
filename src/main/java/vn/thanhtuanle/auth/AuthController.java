package vn.thanhtuanle.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    private static final String ACCESS_TOKEN_COOKIE = "accessToken";
    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    private static final int ACCESS_TOKEN_MAX_AGE = 24 * 60 * 60; // 1 day
    private static final int REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60; // 7 days

    private final AuthService authService;

    /** Set true in prod (HTTPS) so auth cookies carry the Secure flag. */
    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Operation(summary = "Get Google Auth URL", description = "Get URL to redirect user to Google Login")
    @GetMapping("/outbound/google")
    public ApiResponse<Map<String, String>> getGoogleAuthUrl() {
        return ApiResponse.success(Map.of("url", authService.getGoogleAuthUrl()));
    }

    @Operation(summary = "Logout", description = "Logout user and clear cookies")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletResponse response) {
        authService.logout();
        addCookie(response, ACCESS_TOKEN_COOKIE, null, 0);
        addCookie(response, REFRESH_TOKEN_COOKIE, null, 0);
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
        addCookie(response, ACCESS_TOKEN_COOKIE, authResponse.getAccessToken(), ACCESS_TOKEN_MAX_AGE);
        addCookie(response, REFRESH_TOKEN_COOKIE, authResponse.getRefreshToken(), REFRESH_TOKEN_MAX_AGE);
        return ApiResponse.success(authResponse);
    }

    @Operation(summary = "Refresh access token", description = "Exchange a valid refresh token for a new access token")
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(
            @CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String refreshCookie,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletResponse response) {
        String refreshToken = refreshCookie != null ? refreshCookie
                : (body != null ? body.get("refreshToken") : null);
        var authResponse = authService.refreshAccessToken(refreshToken);
        addCookie(response, ACCESS_TOKEN_COOKIE, authResponse.getAccessToken(), ACCESS_TOKEN_MAX_AGE);
        return ApiResponse.success(authResponse);
    }

    @Operation(summary = "Me", description = "Get current user info")
    @GetMapping("/me")
    public ApiResponse<UserResponse> getMe() {
        log.info("Me request received");
        return ApiResponse.success(authService.me());
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }
}
