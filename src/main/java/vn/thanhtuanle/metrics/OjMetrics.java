package vn.thanhtuanle.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.thanhtuanle.common.enums.SubmissionResult;
import vn.thanhtuanle.submission.SubmissionRepository;

import java.time.Duration;
import java.util.List;

@Component
@Slf4j
public class OjMetrics {

    private final MeterRegistry registry;
    private final Timer latencyTimer;

    public OjMetrics(MeterRegistry registry, SubmissionRepository submissionRepository) {
        this.registry = registry;
        this.latencyTimer = Timer.builder("oj.judge.latency")
                .description("Submit-to-verdict latency")
                .publishPercentileHistogram()
                .register(registry);
        Gauge.builder("oj.queue.depth", submissionRepository, r -> queueDepth(r))
                .description("Submissions pending or in judging")
                .register(registry);
    }

    /** Record one terminal verdict. Never throws — must not break the verdict transaction. */
    public void recordVerdict(int status, Duration latency) {
        try {
            latencyTimer.record(latency);
            registry.counter("oj.verdict", "status", statusName(status)).increment();
        } catch (Exception e) {
            log.warn("Failed to record verdict metrics: {}", e.getMessage());
        }
    }

    private static double queueDepth(SubmissionRepository repo) {
        try {
            return repo.countByStatusIn(List.of(
                    SubmissionResult.PENDING.getValue(), SubmissionResult.JUDGING.getValue()));
        } catch (Exception e) {
            return Double.NaN;  // gauge tolerates a transient query failure
        }
    }

    private static String statusName(int status) {
        try {
            return SubmissionResult.fromValue(status).name();
        } catch (IllegalArgumentException e) {
            return "UNKNOWN_" + status;
        }
    }
}
