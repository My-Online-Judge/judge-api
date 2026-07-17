package vn.thanhtuanle.config;

import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import vn.thanhtuanle.auth.TokenBlocklist;
import vn.thanhtuanle.common.util.JwtUtil;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private TokenBlocklist tokenBlocklist;
    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private UserDetails userDetails(String username) {
        return new User(username, "", Collections.emptyList());
    }

    @Test
    void invalidToken_isIgnored_andChainStillProceeds() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("accessToken", "bad.token.value"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(jwtUtil.extractUsername("bad.token.value"))
                .thenThrow(new SignatureException("JWT signature does not match"));

        // Must NOT throw — a bad token must not break the request (public endpoints keep working).
        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void noToken_chainProceedsUnauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtUtil, userDetailsService);
    }

    @Test
    void validToken_authenticates_andCarriesTheTokenAsCredentials() throws Exception {
        String token = "valid.jwt.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("accessToken", token));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        UserDetails principal = userDetails("alice@example.com");
        when(jwtUtil.extractUsername(token)).thenReturn("alice@example.com");
        when(jwtUtil.extractJti(token)).thenReturn("jti-1");
        when(tokenBlocklist.isBlocked("jti-1")).thenReturn(false);
        when(userDetailsService.loadUserByUsername("alice@example.com")).thenReturn(principal);
        when(jwtUtil.isTokenValid(token, principal)).thenReturn(true);

        filter.doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("alice@example.com");
        // The token rides in credentials so logout can extract its jti and revoke it.
        assertThat(auth.getCredentials()).isEqualTo(token);
        verify(chain).doFilter(request, response);
    }

    @Test
    void blocklistedToken_isRejected_evenWhenOtherwiseValid() throws Exception {
        String token = "revoked.jwt.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("accessToken", token));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(jwtUtil.extractUsername(token)).thenReturn("alice@example.com");
        when(jwtUtil.extractJti(token)).thenReturn("jti-revoked");
        when(tokenBlocklist.isBlocked("jti-revoked")).thenReturn(true);

        filter.doFilterInternal(request, response, chain);

        // A revoked token must not authenticate, and we must not even bother loading the user.
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
    }
}
