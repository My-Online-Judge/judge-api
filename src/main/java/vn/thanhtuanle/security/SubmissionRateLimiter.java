package vn.thanhtuanle.security;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import vn.thanhtuanle.common.exception.ErrorCode;
import vn.thanhtuanle.common.exception.RateLimitedException;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Per-user submission cooldown: one atomic SET NX EX per submit. If the key is still
 * there, the user is inside the cooldown and gets a 429 whose Retry-After is the key's
 * remaining TTL. Redis being down fails OPEN — submissions beat enforcement.
 */
@Component
@Slf4j
public class SubmissionRateLimiter {

    static final String KEY_PREFIX = "oj:rl:submit:";

    private final StringRedisTemplate redis;
    private final Counter throttled;
    private final long cooldownSeconds;

    public SubmissionRateLimiter(StringRedisTemplate redis, MeterRegistry registry,
                                 @Value("${oj.submission.cooldown-seconds:10}") long cooldownSeconds) {
        this.redis = redis;
        // Registered at startup, not on first throttle: a lazily-created counter is absent from
        // /actuator/prometheus until it first increments, and Prometheus cannot tell "never happened"
        // from "not exposed" — any alert on it would sit in no-data instead of firing.
        this.throttled = Counter.builder("oj.submission.rate_limited")
                .description("Submissions rejected by the per-user cooldown")
                .register(registry);
        this.cooldownSeconds = cooldownSeconds;
    }

    /** Arms the cooldown atomically; throws 429 with the remaining seconds if still cooling down. */
    public void acquire(UUID userId) {
        String key = KEY_PREFIX + userId;
        Long remaining;
        try {
            Boolean armed = redis.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(cooldownSeconds));
            if (!Boolean.FALSE.equals(armed)) {
                return; // armed now (TRUE) — or a null answer from a flaky proxy: fail open
            }
            remaining = redis.getExpire(key, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Submission limiter unavailable, allowing: {}", e.getMessage());
            return;
        }
        long retryAfter = (remaining == null || remaining < 1) ? 1 : remaining;
        throttled.increment();
        throw new RateLimitedException(ErrorCode.SUBMISSION_RATE_LIMITED, retryAfter);
    }
}
