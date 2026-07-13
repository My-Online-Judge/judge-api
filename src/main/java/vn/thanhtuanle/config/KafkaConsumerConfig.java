package vn.thanhtuanle.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Bounds retries for the {@code submission.judged} consumer so a single
 * poison-pill record (a deserialization failure, or a listener throwing e.g.
 * on a malformed submissionId) cannot stall the pipeline forever. After the
 * backoff is exhausted, the default recoverer logs the failure and the
 * container seeks past the bad record so subsequent messages keep flowing.
 */
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public DefaultErrorHandler defaultErrorHandler() {
        // Retry twice, 1s apart, then give up on the record.
        return new DefaultErrorHandler(new FixedBackOff(1000L, 2L));
    }
}
