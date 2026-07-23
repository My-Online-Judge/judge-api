package vn.thanhtuanle.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins the publish-after-commit ordering: the verdict must NOT be published to subscribers
 * while the consumer's transaction is still open — a subscriber's fresh read in that window
 * would still see PENDING (the update is uncommitted) yet the live publish would already have
 * passed it by, losing the verdict. The publish must fire only in afterCommit().
 */
@ExtendWith(MockitoExtension.class)
class JudgeResultConsumerPublishAfterCommitTest {

    @Mock SubmissionRepository submissionRepository;
    @Mock VerdictPubSub verdictPubSub;
    @Mock SubmissionMapper submissionMapper;
    @Mock OjMetrics ojMetrics;
    @Mock SubmissionDetailAssembler detailAssembler;
    @InjectMocks JudgeResultConsumer consumer;

    @Test
    void withActiveTransaction_publishesOnlyAfterCommit() {
        UUID id = UUID.randomUUID();
        Submission s = Submission.builder()
                .status(SubmissionResult.PENDING.getValue())
                .createdAt(LocalDateTime.now())
                .build();
        s.setId(id);
        when(submissionRepository.findById(id)).thenReturn(Optional.of(s));
        SubmissionResponseDto dto = SubmissionResponseDto.builder()
                .status(SubmissionResult.ACCEPTED.getValue()).build();
        when(submissionMapper.toDto(eq(s), any())).thenReturn(dto);

        SubmissionJudgedEvent e = SubmissionJudgedEvent.builder()
                .submissionId(id.toString()).status(SubmissionResult.ACCEPTED.getValue())
                .result(0).cpuTime(12).realTime(15).memory(3072L).build();

        // Simulate the @Transactional wrapper: synchronization is active during onJudged.
        TransactionSynchronizationManager.initSynchronization();
        try {
            consumer.onJudged(e);

            // The state write happens inside the transaction as before…
            verify(submissionRepository).save(s);
            // …but the publish must NOT have fired yet: the tx has not committed.
            verify(verdictPubSub, never()).publish(any(), any());

            // Simulate the commit completing.
            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCommit();
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(verdictPubSub).publish(id.toString(), dto);
    }

    // The no-transaction path (unit tests, direct calls) publishing immediately is covered by
    // JudgeResultConsumerTest.appliesVerdict_whenPending — not duplicated here.
}
