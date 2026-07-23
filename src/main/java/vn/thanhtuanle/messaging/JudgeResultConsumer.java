package vn.thanhtuanle.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.thanhtuanle.common.enums.SubmissionResult;
import vn.thanhtuanle.entity.Submission;
import vn.thanhtuanle.messaging.event.SubmissionJudgedEvent;
import vn.thanhtuanle.metrics.OjMetrics;
import vn.thanhtuanle.submission.SubmissionDetailAssembler;
import vn.thanhtuanle.submission.SubmissionRepository;
import vn.thanhtuanle.submission.dto.SubmissionResponseDto;
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
    private final SubmissionDetailAssembler detailAssembler;

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
            submission.setDetails(event.getDetails());
            submissionRepository.save(submission);
            publishAfterCommit(event.getSubmissionId(),
                    submissionMapper.toDto(submission, detailAssembler.assemble(submission)));
            ojMetrics.recordVerdict(event.getStatus(), submission.getCreatedAt());
            log.info("Applied verdict {} to submission {}", event.getStatus(), id);
        } finally {
            MDC.remove("submissionId");
        }
    }

    /**
     * Publish the verdict to subscribers only AFTER the surrounding transaction commits.
     * Publishing inside the transaction opens a window where a subscriber's fresh re-read
     * (SubmissionService.streamVerdict) still sees the uncommitted-as-PENDING row while the
     * live publish has already passed its emitter by — the verdict would be lost until the
     * registry timeout. Post-commit, either the subscriber registered before the publish
     * (receives it live) or its re-read observes the committed terminal row (replays).
     * With no active transaction (e.g. unit tests calling onJudged directly), publish
     * immediately — there is no commit to wait for.
     */
    private void publishAfterCommit(String submissionId, SubmissionResponseDto dto) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    verdictPubSub.publish(submissionId, dto);
                }
            });
        } else {
            verdictPubSub.publish(submissionId, dto);
        }
    }
}
