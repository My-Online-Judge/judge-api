package vn.thanhtuanle.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.thanhtuanle.common.enums.SubmissionResult;
import vn.thanhtuanle.submission.SubmissionRepository;

import java.time.Duration;
import java.time.LocalDateTime;
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
        // Pre-create one series per terminal verdict so each exists at 0 from boot. A tagged counter
        // is created on first increment, so before the first SYSTEM_ERROR ever happens there is no
        // oj_verdict_total{status="SYSTEM_ERROR"} series at all — and an alert on a series that does
        // not exist evaluates to no-data, never to "fine". PENDING/JUDGING are excluded: they are
        // queue states that recordVerdict never sees.
        for (SubmissionResult result : SubmissionResult.values()) {
            if (SubmissionResult.isTerminal(result.getValue())) {
                registry.counter("oj.verdict", "status", result.name());
            }
        }
    }

    /** Record one terminal verdict. Never throws — must not break the verdict transaction. */
    public void recordVerdict(int status, LocalDateTime createdAt) {
        try {
            latencyTimer.record(Duration.between(createdAt, LocalDateTime.now()));
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
