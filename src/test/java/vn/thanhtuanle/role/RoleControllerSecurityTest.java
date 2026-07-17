package vn.thanhtuanle.role;

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
import vn.thanhtuanle.auth.TokenBlocklist;
import vn.thanhtuanle.config.SecurityConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RoleController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class,
        CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
@ActiveProfiles("test")
class RoleControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoleService roleService;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private UserDetailsService userDetailsService;
    @MockBean
    private TokenBlocklist tokenBlocklist;

    @Test
    @WithMockUser(authorities = "role:read")
    void allowed_withRoleRead() throws Exception {
        mockMvc.perform(get("/api/v1/roles")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void forbidden_withOnlyAdminAuthority() throws Exception {
        mockMvc.perform(get("/api/v1/roles")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "permission:read")
    void forbidden_withWrongPermission() throws Exception {
        mockMvc.perform(get("/api/v1/roles")).andExpect(status().isForbidden());
    }
}
