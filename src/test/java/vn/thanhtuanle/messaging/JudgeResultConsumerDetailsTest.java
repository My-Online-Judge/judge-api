package vn.thanhtuanle.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.thanhtuanle.common.enums.SubmissionResult;
import vn.thanhtuanle.entity.Submission;
import vn.thanhtuanle.judge.dto.JudgeResultDto;
import vn.thanhtuanle.messaging.event.SubmissionJudgedEvent;
import vn.thanhtuanle.metrics.OjMetrics;
import vn.thanhtuanle.submission.SubmissionDetailAssembler;
import vn.thanhtuanle.submission.SubmissionRepository;
import vn.thanhtuanle.submission.dto.SubmissionResponseDto;
import vn.thanhtuanle.submission.mapper.SubmissionMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JudgeResultConsumerDetailsTest {

    @Mock SubmissionRepository submissionRepository;
    @Mock VerdictPubSub verdictPubSub;
    @Mock SubmissionMapper submissionMapper;
    @Mock OjMetrics ojMetrics;
    @Mock SubmissionDetailAssembler detailAssembler;
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
    void persistsDetails_fromEvent() {
        UUID id = UUID.randomUUID();
        Submission s = pending(id);
        when(submissionRepository.findById(id)).thenReturn(Optional.of(s));
        when(submissionMapper.toDto(any(Submission.class), any()))
                .thenReturn(SubmissionResponseDto.builder().build());

        JudgeResultDto case1 = JudgeResultDto.builder()
                .testCase("1").result(0).cpuTime(12).realTime(15).memory(3072L).build();
        JudgeResultDto case2 = JudgeResultDto.builder()
                .testCase("2").result(-1).cpuTime(9).realTime(11).memory(3000L).build();

        SubmissionJudgedEvent e = SubmissionJudgedEvent.builder()
                .submissionId(id.toString())
                .status(SubmissionResult.WRONG_ANSWER.getValue())
                .result(-1).cpuTime(12).realTime(15).memory(3072L)
                .details(List.of(case1, case2))
                .build();

        consumer.onJudged(e);

        assertThat(s.getDetails()).hasSize(2);
        assertThat(s.getDetails().get(1).getTestCase()).isEqualTo("2");
        assertThat(s.getDetails().get(1).getResult()).isEqualTo(-1);
    }

    @Test
    void persistsEmptyDetails_onCompileError() {
        UUID id = UUID.randomUUID();
        Submission s = pending(id);
        when(submissionRepository.findById(id)).thenReturn(Optional.of(s));
        when(submissionMapper.toDto(any(Submission.class), any()))
                .thenReturn(SubmissionResponseDto.builder().build());

        SubmissionJudgedEvent e = SubmissionJudgedEvent.builder()
                .submissionId(id.toString())
                .status(SubmissionResult.COMPILE_ERROR.getValue())
                .errorMessage("CompileError: expected ';'")
                .details(List.of())
                .build();

        consumer.onJudged(e);

        assertThat(s.getDetails()).isEmpty();
    }
}
