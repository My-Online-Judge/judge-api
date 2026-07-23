package vn.thanhtuanle.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import vn.thanhtuanle.common.enums.BanType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessBanFilterTest {

    @Mock AccessBanMirror mirror;
    @Mock FilterChain chain;

    AccessBanFilter filter;
    MockHttpServletRequest request;
    MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new AccessBanFilter(mirror, new ObjectMapper(), new SimpleMeterRegistry());
        request = new MockHttpServletRequest("GET", "/api/v1/problems");
        request.setRemoteAddr("1.2.3.4");
        request.addHeader("X-Device-Id", "dev-1");
        request.addHeader("User-Agent", "probe/1.0");
        response = new MockHttpServletResponse();
    }

    @Test
    void cleanRequest_passesThrough() throws Exception {
        when(mirror.isBanned(any(), any())).thenReturn(false);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void bannedIp_gets403Json_andChainStops() throws Exception {
        when(mirror.isBanned(eq(BanType.IP), eq("1.2.3.4"))).thenReturn(true);
        filter.doFilter(request, response, chain);
        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString()).contains("banned");
    }

    @Test
    void bannedDevice_gets403() throws Exception {
        when(mirror.isBanned(eq(BanType.IP), any())).thenReturn(false);
        when(mirror.isBanned(eq(BanType.DEVICE), eq("dev-1"))).thenReturn(true);
        filter.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(403);
    }
}
