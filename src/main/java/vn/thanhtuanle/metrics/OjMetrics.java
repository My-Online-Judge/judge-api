package vn.thanhtuanle.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class OjMetrics {

    public OjMetrics(MeterRegistry registry) {
        // meters registered in Task 2
    }

    /** Record one terminal verdict. Never throws. */
    public void recordVerdict(int status, Duration latency) {
        // implemented in Task 2
    }
}
