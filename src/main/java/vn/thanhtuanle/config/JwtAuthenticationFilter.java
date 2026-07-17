package vn.thanhtuanle.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.thanhtuanle.auth.TokenBlocklist;
import vn.thanhtuanle.common.util.JwtUtil;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final TokenBlocklist tokenBlocklist;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        String token = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String userName = jwtUtil.extractUsername(token);
            if (userName != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Cheap Redis check first: a revoked token is refused without touching the DB.
                if (tokenBlocklist.isBlocked(jwtUtil.extractJti(token))) {
                    log.debug("Rejecting revoked (blocklisted) token for {}", userName);
                } else {
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(userName);
                    if (jwtUtil.isTokenValid(token, userDetails)) {
                        // The raw token rides in credentials so logout can extract its jti and revoke it.
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                token,
                                userDetails.getAuthorities());
                        authToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
        } catch (Exception ex) {
            // Invalid / expired / malformed token, or unknown user: ignore it and continue
            // unauthenticated so public (whitelisted) endpoints keep working and protected
            // endpoints return a clean 401 via the entry point instead of a 500 from the filter.
            log.debug("Ignoring invalid JWT token: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
