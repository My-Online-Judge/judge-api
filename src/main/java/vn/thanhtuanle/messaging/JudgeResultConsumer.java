package vn.thanhtuanle.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.thanhtuanle.common.enums.SubmissionResult;
import vn.thanhtuanle.entity.Submission;
import vn.thanhtuanle.messaging.event.SubmissionJudgedEvent;
import vn.thanhtuanle.metrics.OjMetrics;
import vn.thanhtuanle.submission.SubmissionRepository;
import vn.thanhtuanle.submission.mapper.SubmissionMapper;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JudgeResultConsumer {

    private final SubmissionRepository submissionRepository;
    private final VerdictPubSub verdictPubSub;
    private final SubmissionMapper submissionMapper;
    private final OjMetrics ojMetrics;

    @KafkaListener(topics = KafkaTopics.SUBMISSION_JUDGED, groupId = "judge-api-results")
    @Transactional
    public void onJudged(SubmissionJudgedEvent event) {
        MDC.put("submissionId", event.getSubmissionId());
        try {
            UUID id = UUID.fromString(event.getSubmissionId());
            Submission submission = submissionRepository.findById(id).orElse(null);
            if (submission == null) {
                log.warn("Received verdict for unknown submission {}", id);
                return;
            }
            // Idempotency: only the first terminal verdict wins; duplicates are ignored.
            if (SubmissionResult.isTerminal(submission.getStatus())) {
                log.info("Submission {} already finished (status={}), ignoring verdict",
                        id, submission.getStatus());
                return;
            }
            if (event.getStatus() == null) {
                log.warn("Received verdict with null status for submission {}, ignoring", id);
                return;
            }
            submission.setStatus(event.getStatus());
            submission.setResult(event.getResult());
            submission.setCpuTime(event.getCpuTime());
            submission.setTime(event.getRealTime());
            submission.setMemory(event.getMemory());
            submission.setErrorMessage(event.getErrorMessage());
            submissionRepository.save(submission);
            verdictPubSub.publish(event.getSubmissionId(), submissionMapper.toDto(submission));
            log.info("Applied verdict {} to submission {}", event.getStatus(), id);
        } finally {
            MDC.remove("submissionId");
        }
    }
}
