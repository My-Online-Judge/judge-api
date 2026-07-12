package vn.thanhtuanle.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.thanhtuanle.messaging.event.SubmissionRequestedAppEvent;

/**
 * Republishes {@code submission.requested} to Kafka only after the enclosing
 * database transaction has committed, so the judge worker can never race
 * ahead of the submission row it needs to update.
 */
@Component
@RequiredArgsConstructor
public class SubmissionRequestedTxListener {

    private final SubmissionEventPublisher submissionEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAfterCommit(SubmissionRequestedAppEvent appEvent) {
        submissionEventPublisher.publishRequested(appEvent.payload());
    }
}
