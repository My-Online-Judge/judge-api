package vn.thanhtuanle.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import vn.thanhtuanle.messaging.event.SubmissionRequestedEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubmissionEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishRequested(SubmissionRequestedEvent event) {
        MDC.put("submissionId", event.getSubmissionId());
        try {
            log.info("Publishing submission.requested for {}", event.getSubmissionId());
            kafkaTemplate.send(KafkaTopics.SUBMISSION_REQUESTED, event.getSubmissionId(), event);
        } finally {
            MDC.remove("submissionId");
        }
    }
}
