package vn.thanhtuanle.role;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import vn.thanhtuanle.role.dto.RoleResponse;
import vn.thanhtuanle.user.RoleRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    public List<RoleResponse> listRoles() {
        return roleMapper.toResponses(roleRepository.findAll(Sort.by("name")));
    }
}
