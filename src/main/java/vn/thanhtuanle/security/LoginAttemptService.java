package vn.thanhtuanle.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.thanhtuanle.common.util.ClientMeta;
import vn.thanhtuanle.entity.LoginAttempt;

import java.time.LocalDateTime;

/** Audit log of password/Google login attempts. Refresh-token calls are NOT logged (volume). */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptService {

    static final int RETENTION_DAYS = 30;

    private final LoginAttemptRepository repository;

    /** Never throws — the audit trail must not break login itself. */
    public void record(String username, ClientMeta meta, boolean success, String errorCode) {
        try {
            repository.save(LoginAttempt.builder()
                    .username(username)
                    .ip(meta.ip())
                    .deviceHash(meta.deviceHash())
                    .userAgent(meta.userAgent())
                    .success(success)
                    .errorCode(errorCode)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to record login attempt for {}: {}", username, e.getMessage());
        }
    }

    /** Nightly retention sweep (03:30 VN time — the app pins Asia/Ho_Chi_Minh). */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void prune() {
        long removed = repository.deleteByCreatedAtBefore(LocalDateTime.now().minusDays(RETENTION_DAYS));
        if (removed > 0) {
            log.info("Pruned {} login attempts older than {} days", removed, RETENTION_DAYS);
        }
    }
}
