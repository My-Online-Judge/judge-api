package vn.thanhtuanle.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.thanhtuanle.common.enums.SubmissionResult;
import vn.thanhtuanle.entity.Submission;
import vn.thanhtuanle.messaging.event.SubmissionJudgedEvent;
import vn.thanhtuanle.metrics.OjMetrics;
import vn.thanhtuanle.submission.SubmissionRepository;
import vn.thanhtuanle.submission.dto.SubmissionResponseDto;
import vn.thanhtuanle.submission.mapper.SubmissionMapper;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JudgeResultConsumerTest {

    @Mock SubmissionRepository submissionRepository;
    @Mock VerdictPubSub verdictPubSub;
    @Mock SubmissionMapper submissionMapper;
    @Mock OjMetrics ojMetrics;
    @InjectMocks JudgeResultConsumer consumer;

    private Submission pending(UUID id) {
        Submission s = Submission.builder()
                .status(SubmissionResult.PENDING.getValue())
                .createdAt(LocalDateTime.now())
                .build();
        s.setId(id);
        return s;
    }

    @Test
    void appliesVerdict_whenPending() {
        UUID id = UUID.randomUUID();
        Submission s = pending(id);
        when(submissionRepository.findById(id)).thenReturn(Optional.of(s));
        SubmissionResponseDto dto = SubmissionResponseDto.builder()
                .status(SubmissionResult.ACCEPTED.getValue()).build();
        when(submissionMapper.toDto(s)).thenReturn(dto);

        SubmissionJudgedEvent e = SubmissionJudgedEvent.builder()
                .submissionId(id.toString()).status(SubmissionResult.ACCEPTED.getValue())
                .result(0).cpuTime(12).realTime(15).memory(3072L).build();

        consumer.onJudged(e);

        assertThat(s.getStatus()).isEqualTo(SubmissionResult.ACCEPTED.getValue());
        assertThat(s.getCpuTime()).isEqualTo(12);
        assertThat(s.getTime()).isEqualTo(15);
        assertThat(s.getMemory()).isEqualTo(3072L);
        verify(submissionRepository).save(s);
        verify(verdictPubSub).publish(id.toString(), dto);
    }

    @Test
    void ignoresResult_whenStatusIsNull() {
        UUID id = UUID.randomUUID();
        Submission s = pending(id);
        when(submissionRepository.findById(id)).thenReturn(Optional.of(s));

        SubmissionJudgedEvent e = SubmissionJudgedEvent.builder()
                .submissionId(id.toString()).status(null).build();

        consumer.onJudged(e);

        assertThat(s.getStatus()).isEqualTo(SubmissionResult.PENDING.getValue());
        verify(submissionRepository, never()).save(any());
        verify(verdictPubSub, never()).publish(any(), any());
    }

    @Test
    void ignoresResult_whenAlreadyFinished() {
        UUID id = UUID.randomUUID();
        Submission s = Submission.builder().status(SubmissionResult.ACCEPTED.getValue()).build();
        s.setId(id);
        when(submissionRepository.findById(id)).thenReturn(Optional.of(s));

        SubmissionJudgedEvent e = SubmissionJudgedEvent.builder()
                .submissionId(id.toString()).status(SubmissionResult.WRONG_ANSWER.getValue()).build();

        consumer.onJudged(e);

        assertThat(s.getStatus()).isEqualTo(SubmissionResult.ACCEPTED.getValue());
        verify(submissionRepository, never()).save(any());
        verify(verdictPubSub, never()).publish(any(), any());
    }
}
