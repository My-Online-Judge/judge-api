package vn.thanhtuanle.problem;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import vn.thanhtuanle.common.util.JwtUtil;
import vn.thanhtuanle.config.CustomAccessDeniedHandler;
import vn.thanhtuanle.config.CustomAuthenticationEntryPoint;
import vn.thanhtuanle.config.JwtAuthenticationFilter;
import vn.thanhtuanle.config.SecurityConfig;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TestCaseController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class,
        CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
@ActiveProfiles("test")
class TestCaseControllerSecurityTest {

    private static final String BASE = "/api/v1/problems/some-slug/test-cases";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TestCaseService testCaseService;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(authorities = "problem:update")
    void listAllowed_whenUserHasProblemUpdatePermission() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void listForbidden_whenUserHasOnlyAdminRoleAuthority() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "problem:create")
    void listForbidden_whenUserHasWrongPermission() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "problem:update")
    void deleteAllowed_whenUserHasProblemUpdatePermission() throws Exception {
        mockMvc.perform(delete(BASE + "/" + UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "role:read")
    void deleteForbidden_whenUserHasWrongPermission() throws Exception {
        mockMvc.perform(delete(BASE + "/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }
}
