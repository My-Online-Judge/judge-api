package vn.thanhtuanle.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import vn.thanhtuanle.submission.SubmissionRepository;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OjMetricsQueueDepthTest {

    @Test
    void queueDepthGauge_reflectsPendingAndJudgingCount() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SubmissionRepository repo = mock(SubmissionRepository.class);
        when(repo.countByStatusIn(anyCollection())).thenReturn(4L);

        new OjMetrics(registry, repo);  // constructor registers the gauge

        assertThat(registry.get("oj.queue.depth").gauge().value()).isEqualTo(4.0);
    }
}
