package vn.thanhtuanle.permission;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.thanhtuanle.entity.Permission;

import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {
}
