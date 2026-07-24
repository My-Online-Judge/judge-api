package vn.thanhtuanle.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import vn.thanhtuanle.common.enums.SubmissionResult;
import vn.thanhtuanle.security.AccessBanFilter;
import vn.thanhtuanle.security.AccessBanMirror;
import vn.thanhtuanle.security.LoginRateLimiter;
import vn.thanhtuanle.security.SubmissionRateLimiter;
import vn.thanhtuanle.submission.SubmissionRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every oj.* counter must exist at 0 the moment its owner is constructed — before anything
 * increments it.
 *
 * <p>Micrometer creates a counter lazily on the first {@code registry.counter(...)} call, so a
 * counter that has never fired is simply absent from /actuator/prometheus. Prometheus cannot tell
 * that apart from "the app stopped exposing this", and an alert whose series does not exist sits in
 * no-data instead of firing. These assertions fail the build if someone reverts a constructor
 * registration back to an increment-site lookup.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MetricPreRegistrationTest {

    @Mock StringRedisTemplate redis;
    @Mock AccessBanMirror mirror;
    @Mock SubmissionRepository submissionRepository;

    MeterRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
    }

    @Test
    void loginRateLimiterRegistersItsCounterAtZero() {
        new LoginRateLimiter(redis, registry);

        assertThat(registry.find("oj.login.rate_limited").counter())
                .isNotNull()
                .extracting(c -> c.count())
                .isEqualTo(0.0d);
    }

    @Test
    void submissionRateLimiterRegistersItsCounterAtZero() {
        new SubmissionRateLimiter(redis, registry, 10);

        assertThat(registry.find("oj.submission.rate_limited").counter())
                .isNotNull()
                .extracting(c -> c.count())
                .isEqualTo(0.0d);
    }

    @Test
    void accessBanFilterRegistersItsCounterAtZero() {
        new AccessBanFilter(mirror, new ObjectMapper(), registry);

        assertThat(registry.find("oj.request.banned").counter())
                .isNotNull()
                .extracting(c -> c.count())
                .isEqualTo(0.0d);
    }

    @Test
    void ojMetricsRegistersOneVerdictSeriesPerTerminalStatus() {
        new OjMetrics(registry, submissionRepository);

        for (SubmissionResult result : SubmissionResult.values()) {
            var counter = registry.find("oj.verdict").tag("status", result.name()).counter();
            if (SubmissionResult.isTerminal(result.getValue())) {
                assertThat(counter)
                        .as("terminal verdict %s must have a series at boot", result)
                        .isNotNull();
                assertThat(counter.count()).isEqualTo(0.0d);
            } else {
                assertThat(counter)
                        .as("%s is a queue state, not a verdict — it must not get a series", result)
                        .isNull();
            }
        }
    }
}
