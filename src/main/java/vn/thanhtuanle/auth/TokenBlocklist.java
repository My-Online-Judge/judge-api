package vn.thanhtuanle.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis-backed revocation list for access tokens, keyed by the token's {@code jti}.
 *
 * <p>On logout a token's jti is parked here with a TTL equal to the token's remaining lifetime, so
 * the entry self-evicts exactly when the token would have expired anyway — the list never grows
 * unbounded. {@link JwtAuthenticationFilter} consults it on every request and refuses a blocklisted
 * token, which is what makes logout (and short-TTL access tokens) actually invalidate a token that
 * is otherwise still signature-valid.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenBlocklist {

    static final String KEY_PREFIX = "oj:token:blocklist:";

    private final StringRedisTemplate redisTemplate;

    /**
     * Revoke {@code jti} until {@code ttlMillis} from now. A null jti or a non-positive TTL (an
     * already-expired token) is a no-op — the filter's own expiry check already rejects those.
     */
    public void block(String jti, long ttlMillis) {
        if (jti == null || ttlMillis <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + jti, "1", Duration.ofMillis(ttlMillis));
    }

    /** True if this jti has been revoked and not yet expired out of Redis. */
    public boolean isBlocked(String jti) {
        if (jti == null) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
    }
}
