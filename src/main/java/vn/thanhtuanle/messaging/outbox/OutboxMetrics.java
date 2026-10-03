package vn.thanhtuanle.messaging.outbox;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * {@code oj.outbox.pending} and {@code oj.outbox.oldest.age} (seconds). A healthy relay keeps the
 * oldest row under a few seconds; alert {@code OutboxBacklogStale} fires past a minute.
 */
@Component
public class OutboxMetrics {

    public OutboxMetrics(MeterRegistry registry, OutboxRepository repository) {
        Gauge.builder("oj.outbox.pending", repository, OutboxMetrics::pending)
                .description("Outbox rows not yet sent to Kafka")
                .register(registry);
        Gauge.builder("oj.outbox.oldest.age", repository, OutboxMetrics::oldestAgeSeconds)
                .description("Age of the oldest outbox row not yet sent to Kafka")
                .baseUnit("seconds")
                .register(registry);
    }

    static double pending(OutboxRepository repository) {
        try {
            return repository.countByPublishedAtIsNull();
        } catch (Exception e) {
            return Double.NaN;  // a gauge tolerates a transient query failure
        }
    }

    static double oldestAgeSeconds(OutboxRepository repository) {
        try {
            LocalDateTime oldest = repository.oldestUnpublishedCreatedAt();
            return oldest == null ? 0 : Duration.between(oldest, LocalDateTime.now()).toMillis() / 1000.0;
        } catch (Exception e) {
            return Double.NaN;
        }
    }
}
