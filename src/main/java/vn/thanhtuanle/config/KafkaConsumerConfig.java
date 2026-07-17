package vn.thanhtuanle.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import vn.thanhtuanle.messaging.KafkaTopics;

/**
 * Bounds retries for the {@code submission.judged} consumer so a single poison-pill record (a
 * deserialization failure, or a listener throwing e.g. on a malformed submissionId) cannot stall
 * the pipeline forever. After the backoff is exhausted the record is republished to the dead-letter
 * topic {@link KafkaTopics#SUBMISSION_JUDGED_DLQ} — captured for inspection/replay rather than
 * silently dropped — and the container seeks past it so subsequent messages keep flowing.
 */
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public DefaultErrorHandler defaultErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(KafkaTopics.SUBMISSION_JUDGED_DLQ, record.partition()));
        // Retry twice, 1s apart, then dead-letter the record.
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));
    }
}
