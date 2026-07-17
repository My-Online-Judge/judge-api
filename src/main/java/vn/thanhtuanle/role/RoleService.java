package vn.thanhtuanle.role;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.thanhtuanle.common.exception.AppException;
import vn.thanhtuanle.common.exception.ErrorCode;
import vn.thanhtuanle.entity.Permission;
import vn.thanhtuanle.entity.Role;
import vn.thanhtuanle.permission.PermissionRepository;
import vn.thanhtuanle.role.dto.RoleResponse;
import vn.thanhtuanle.user.RoleRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleService {

    private static final String ADMIN_ROLE = "ADMIN";

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper roleMapper;

    public List<RoleResponse> listRoles() {
        return roleMapper.toResponses(roleRepository.findAll(Sort.by("name")));
    }

    /**
     * Replace a role's permission set with the given permission names (replace-all, idempotent).
     * The ADMIN role is immutable — it always carries every permission (managed by migrations).
     */
    @Transactional
    public RoleResponse updatePermissions(UUID roleId, Set<String> permissionNames) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));

        if (ADMIN_ROLE.equals(role.getName())) {
            throw new AppException(ErrorCode.ROLE_IMMUTABLE);
        }

        Set<String> requested = permissionNames == null ? Set.of() : permissionNames;
        List<Permission> resolved = requested.isEmpty()
                ? List.of()
                : permissionRepository.findByNameIn(requested);
        if (resolved.size() != requested.size()) {
            throw new AppException(ErrorCode.PERMISSION_NOT_EXISTED);
        }

        role.setPermissions(new HashSet<>(resolved));
        roleRepository.save(role);
        return roleMapper.toResponse(role);
    }
}
