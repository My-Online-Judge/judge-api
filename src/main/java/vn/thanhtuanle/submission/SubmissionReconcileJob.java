package vn.thanhtuanle.submission;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.thanhtuanle.common.enums.SubmissionResult;
import vn.thanhtuanle.entity.Submission;
import vn.thanhtuanle.messaging.VerdictPubSub;
import vn.thanhtuanle.submission.mapper.SubmissionMapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Safety net for the "no verdict ever arrived" case: a worker/broker/sandbox died, or a
 * submission.requested message was lost, so the submission would otherwise sit in PENDING/JUDGING
 * forever. Periodically flips any such submission older than {@code judge.stuck-timeout-min} to
 * SYSTEM_ERROR and pushes the verdict to any local SSE subscriber.
 *
 * <p>Ordering vs. a late real verdict is resolved by {@code JudgeResultConsumer}'s terminal-status
 * idempotency check: once reconcile writes SYSTEM_ERROR (a terminal status), a verdict that arrives
 * afterwards is ignored.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubmissionReconcileJob {

    private final SubmissionRepository submissionRepository;
    private final VerdictPubSub verdictPubSub;
    private final SubmissionMapper submissionMapper;

    @Value("${judge.stuck-timeout-min:5}")
    private long stuckTimeoutMin;

    @Scheduled(fixedDelayString = "${judge.reconcile-interval-ms:60000}")
    @Transactional
    public void reconcileStuck() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(stuckTimeoutMin);
        List<Submission> stuck = submissionRepository.findStuck(
                List.of(SubmissionResult.PENDING.getValue(), SubmissionResult.JUDGING.getValue()),
                threshold);
        if (stuck.isEmpty()) {
            return;
        }
        for (Submission submission : stuck) {
            submission.setStatus(SubmissionResult.SYSTEM_ERROR.getValue());
            submission.setErrorMessage(
                    "Judging timed out: no verdict within " + stuckTimeoutMin + " minutes");
            submissionRepository.save(submission);
            verdictPubSub.publish(submission.getId().toString(), submissionMapper.toDto(submission));
            log.warn("Reconciled stuck submission {} -> SYSTEM_ERROR", submission.getId());
        }
        log.info("Reconcile flipped {} stuck submission(s) to SYSTEM_ERROR", stuck.size());
    }
}
