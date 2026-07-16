package vn.thanhtuanle.role;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.thanhtuanle.common.constant.Routes;
import vn.thanhtuanle.common.payload.ApiResponse;
import vn.thanhtuanle.role.dto.RoleResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "List all roles with their permissions (admin)")
    @GetMapping(Routes.ROLES)
    @PreAuthorize("hasAuthority('role:read')")
    public ApiResponse<List<RoleResponse>> listRoles() {
        return ApiResponse.success(roleService.listRoles());
    }
}
