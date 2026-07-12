package vn.thanhtuanle.judgeserver;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.thanhtuanle.entity.JudgeServer;

import java.util.Optional;
import java.util.UUID;

public interface JudgeServerRepository extends JpaRepository<JudgeServer, UUID> {
    Optional<JudgeServer> findByHostname(String hostname);
}
