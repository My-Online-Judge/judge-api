package vn.thanhtuanle.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.thanhtuanle.entity.User;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID id);

    Optional<User> findByEmail(String email);

    long countByRoles_Id(UUID roleId);

    /**
     * Admin user search. All filters are optional; {@code search} must be lower-cased by the
     * caller (matched case-insensitively against username/email/name). When {@code status} is
     * omitted, soft-deleted users (status {@code 2}) are hidden; pass {@code status = 2} to
     * list them. {@code roleId} matches users holding that role. {@code createdFrom}/{@code
     * createdTo} bound {@code createdAt}. Uses an EXISTS subquery (not a fetch join) so
     * pagination stays correct against the EAGER {@code roles} collection.
     */
    @Query("""
            SELECT u FROM User u
            WHERE (:search IS NULL OR LOWER(u.username) LIKE CONCAT('%', :search, '%')
                   OR LOWER(u.email) LIKE CONCAT('%', :search, '%')
                   OR LOWER(u.name) LIKE CONCAT('%', :search, '%'))
              AND (:status IS NULL OR u.status = :status)
              AND (:status IS NOT NULL OR u.status <> 2)
              AND (:roleId IS NULL OR EXISTS (SELECT 1 FROM u.roles r WHERE r.id = :roleId))
              AND (:createdFrom IS NULL OR u.createdAt >= :createdFrom)
              AND (:createdTo IS NULL OR u.createdAt < :createdTo)
            """)
    Page<User> search(@Param("search") String search,
                      @Param("status") Integer status,
                      @Param("roleId") UUID roleId,
                      @Param("createdFrom") LocalDateTime createdFrom,
                      @Param("createdTo") LocalDateTime createdTo,
                      Pageable pageable);
}
