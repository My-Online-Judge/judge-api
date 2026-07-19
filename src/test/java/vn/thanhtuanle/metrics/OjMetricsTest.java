package vn.thanhtuanle.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import vn.thanhtuanle.common.enums.SubmissionResult;
import vn.thanhtuanle.submission.SubmissionRepository;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OjMetricsTest {

    @Test
    void recordVerdict_recordsLatencyAndTaggedCounter() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OjMetrics metrics = new OjMetrics(registry, mock(SubmissionRepository.class));

        metrics.recordVerdict(SubmissionResult.ACCEPTED.getValue(), Duration.ofMillis(1500));

        assertThat(registry.get("oj.judge.latency").timer().count()).isEqualTo(1);
        assertThat(registry.get("oj.verdict").tag("status", "ACCEPTED").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordVerdict_neverThrowsOnBadStatus() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OjMetrics metrics = new OjMetrics(registry, mock(SubmissionRepository.class));
        // 999 is not a valid SubmissionResult value — must not throw.
        metrics.recordVerdict(999, Duration.ofMillis(10));
        assertThat(registry.get("oj.judge.latency").timer().count()).isEqualTo(1);
    }
}
