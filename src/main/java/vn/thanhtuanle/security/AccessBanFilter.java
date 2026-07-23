package vn.thanhtuanle.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.thanhtuanle.common.enums.BanType;
import vn.thanhtuanle.common.exception.ErrorCode;
import vn.thanhtuanle.common.payload.ApiResponse;
import vn.thanhtuanle.common.util.ClientMeta;

import java.io.IOException;

/**
 * Hard access ban by ip or device_hash, enforced BEFORE the Spring Security chain (see the
 * FilterRegistrationBean in SecurityConfig) so bans also cover permitAll endpoints.
 * Runs outside @ControllerAdvice, so it writes its own 403 JSON body.
 *
 * <p>NOT a @Component on purpose: @WebMvcTest slices component-scan every Filter bean,
 * which would drag the Redis-backed mirror into controller security tests. The bean is
 * created in {@link AccessBanFilterConfig} instead.
 */
@RequiredArgsConstructor
public class AccessBanFilter extends OncePerRequestFilter {

    private final AccessBanMirror mirror;
    private final ObjectMapper objectMapper;
    private final MeterRegistry registry;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        ClientMeta meta = ClientMeta.from(request);
        if (mirror.isBanned(BanType.IP, meta.ip()) || mirror.isBanned(BanType.DEVICE, meta.deviceHash())) {
            registry.counter("oj.request.banned").increment();
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(),
                    ApiResponse.error(HttpStatus.FORBIDDEN.value(), ErrorCode.ACCESS_BANNED.getMessage()));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
