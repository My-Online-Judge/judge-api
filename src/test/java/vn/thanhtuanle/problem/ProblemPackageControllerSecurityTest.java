package vn.thanhtuanle.problem;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import vn.thanhtuanle.auth.TokenBlocklist;
import vn.thanhtuanle.common.exception.ResourceAlreadyExistException;
import vn.thanhtuanle.common.util.JwtUtil;
import vn.thanhtuanle.config.CustomAccessDeniedHandler;
import vn.thanhtuanle.config.CustomAuthenticationEntryPoint;
import vn.thanhtuanle.config.JwtAuthenticationFilter;
import vn.thanhtuanle.config.SecurityConfig;
import vn.thanhtuanle.problem.dto.ProblemResponseDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProblemPackageController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class,
        CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
@ActiveProfiles("test")
class ProblemPackageControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProblemPackageService problemPackageService;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private UserDetailsService userDetailsService;
    @MockBean
    private TokenBlocklist tokenBlocklist;

    private static final MockMultipartFile PKG =
            new MockMultipartFile("file", "pkg.zip", "application/zip", new byte[]{1});

    @Test
    @WithMockUser(authorities = "problem:create")
    void importAllowed_withProblemCreate() throws Exception {
        Mockito.when(problemPackageService.importProblem(any(), any()))
                .thenReturn(ProblemResponseDto.builder().build());
        mockMvc.perform(multipart("/api/v1/problems/import").file(PKG))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = "problem:update")
    void importForbidden_withWrongPermission() throws Exception {
        mockMvc.perform(multipart("/api/v1/problems/import").file(PKG))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "problem:create")
    void importConflict_whenSlugAlreadyExists() throws Exception {
        Mockito.when(problemPackageService.importProblem(any(), any()))
                .thenThrow(new ResourceAlreadyExistException("Problem slug already exists: x"));
        mockMvc.perform(multipart("/api/v1/problems/import").file(PKG))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = "problem:update")
    void exportAllowed_withProblemUpdate() throws Exception {
        Mockito.when(problemPackageService.exportProblem(eq("a-plus-b"))).thenReturn(new byte[]{1, 2});
        mockMvc.perform(get("/api/v1/problems/a-plus-b/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"a-plus-b.zip\""));
    }

    @Test
    @WithMockUser(authorities = "problem:create")
    void exportForbidden_withWrongPermission() throws Exception {
        mockMvc.perform(get("/api/v1/problems/a-plus-b/export"))
                .andExpect(status().isForbidden());
    }

    @Test
    void exportForbidden_withoutToken() throws Exception {
        // This app's CustomAuthenticationEntryPoint deliberately returns 403 (not 401) for
        // unauthenticated requests app-wide; see its "Return 403 as requested" comment, and
        // every other *ControllerSecurityTest in this codebase, which only ever asserts 403.
        mockMvc.perform(get("/api/v1/problems/a-plus-b/export"))
                .andExpect(status().isForbidden());
    }
}
