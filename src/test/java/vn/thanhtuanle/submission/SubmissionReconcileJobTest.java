package vn.thanhtuanle.submission;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.thanhtuanle.common.enums.SubmissionResult;
import vn.thanhtuanle.entity.Submission;
import vn.thanhtuanle.messaging.VerdictPubSub;
import vn.thanhtuanle.submission.dto.SubmissionResponseDto;
import vn.thanhtuanle.submission.mapper.SubmissionMapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionReconcileJobTest {

    @Mock SubmissionRepository submissionRepository;
    @Mock VerdictPubSub verdictPubSub;
    @Mock SubmissionMapper submissionMapper;
    @Mock SubmissionDetailAssembler detailAssembler;
    @InjectMocks SubmissionReconcileJob job;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(job, "stuckTimeoutMin", 5L);
    }

    @Test
    void flipsStuckSubmissionToSystemError_andNotifiesSse() {
        UUID id = UUID.randomUUID();
        Submission stuck = Submission.builder().status(SubmissionResult.PENDING.getValue()).build();
        stuck.setId(id);
        when(submissionRepository.findStuck(anyCollection(), any(LocalDateTime.class)))
                .thenReturn(List.of(stuck));
        SubmissionResponseDto dto = SubmissionResponseDto.builder()
                .status(SubmissionResult.SYSTEM_ERROR.getValue()).build();
        when(submissionMapper.toDto(eq(stuck), any())).thenReturn(dto);

        job.reconcileStuck();

        assertThat(stuck.getStatus()).isEqualTo(SubmissionResult.SYSTEM_ERROR.getValue());
        assertThat(stuck.getErrorMessage()).contains("timed out");
        verify(submissionRepository).save(stuck);
        verify(verdictPubSub).publishAfterCommit(id.toString(), dto);
    }

    @Test
    void publishesReconciledVerdict_onlyAfterCommit() {
        // reconcileStuck is @Transactional and may flip N submissions before its commit:
        // publishing mid-loop would let a subscriber's fresh re-read still see PENDING while
        // the live publish has already passed it by (same lost-verdict window as the consumer).
        UUID id = UUID.randomUUID();
        Submission stuck = Submission.builder().status(SubmissionResult.PENDING.getValue()).build();
        stuck.setId(id);
        when(submissionRepository.findStuck(anyCollection(), any(LocalDateTime.class)))
                .thenReturn(List.of(stuck));
        SubmissionResponseDto dto = SubmissionResponseDto.builder()
                .status(SubmissionResult.SYSTEM_ERROR.getValue()).build();
        when(submissionMapper.toDto(eq(stuck), any())).thenReturn(dto);

        // Real VerdictPubSub so its after-commit deferral actually runs; publish itself is
        // stubbed out (no Redis in unit tests).
        VerdictPubSub deferringPubSub = spy(new VerdictPubSub(null, null, null));
        doNothing().when(deferringPubSub).publish(any(), any());
        SubmissionReconcileJob txJob = new SubmissionReconcileJob(
                submissionRepository, deferringPubSub, submissionMapper, detailAssembler);
        ReflectionTestUtils.setField(txJob, "stuckTimeoutMin", 5L);

        // Simulate the @Transactional wrapper: synchronization active during reconcileStuck.
        TransactionSynchronizationManager.initSynchronization();
        try {
            txJob.reconcileStuck();

            // State write happened inside the transaction…
            verify(submissionRepository).save(stuck);
            // …but nothing may be published before the commit.
            verify(deferringPubSub, never()).publish(any(), any());

            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCommit();
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(deferringPubSub).publish(id.toString(), dto);
    }

    @Test
    void doesNothing_whenNoStuckSubmissions() {
        when(submissionRepository.findStuck(anyCollection(), any(LocalDateTime.class)))
                .thenReturn(List.of());

        job.reconcileStuck();

        verify(submissionRepository, never()).save(any());
        verify(verdictPubSub, never()).publishAfterCommit(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void queriesOnlyPendingAndJudgingStatuses() {
        when(submissionRepository.findStuck(anyCollection(), any(LocalDateTime.class)))
                .thenReturn(List.of());

        job.reconcileStuck();

        ArgumentCaptor<Collection<Integer>> statuses = ArgumentCaptor.forClass(Collection.class);
        verify(submissionRepository).findStuck(statuses.capture(), any(LocalDateTime.class));
        assertThat(statuses.getValue()).containsExactlyInAnyOrder(
                SubmissionResult.PENDING.getValue(), SubmissionResult.JUDGING.getValue());
    }
}
