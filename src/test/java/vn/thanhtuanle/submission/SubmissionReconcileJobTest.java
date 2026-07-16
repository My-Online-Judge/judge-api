package vn.thanhtuanle.submission;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import vn.thanhtuanle.common.enums.SubmissionResult;
import vn.thanhtuanle.entity.Submission;
import vn.thanhtuanle.submission.dto.SubmissionResponseDto;
import vn.thanhtuanle.submission.mapper.SubmissionMapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionReconcileJobTest {

    @Mock SubmissionRepository submissionRepository;
    @Mock SubmissionSseRegistry sseRegistry;
    @Mock SubmissionMapper submissionMapper;
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
        when(submissionMapper.toDto(stuck)).thenReturn(dto);

        job.reconcileStuck();

        assertThat(stuck.getStatus()).isEqualTo(SubmissionResult.SYSTEM_ERROR.getValue());
        assertThat(stuck.getErrorMessage()).contains("timed out");
        verify(submissionRepository).save(stuck);
        verify(sseRegistry).complete(id.toString(), dto);
    }

    @Test
    void doesNothing_whenNoStuckSubmissions() {
        when(submissionRepository.findStuck(anyCollection(), any(LocalDateTime.class)))
                .thenReturn(List.of());

        job.reconcileStuck();

        verify(submissionRepository, never()).save(any());
        verify(sseRegistry, never()).complete(any(), any());
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
