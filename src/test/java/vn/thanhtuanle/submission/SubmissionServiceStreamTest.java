package vn.thanhtuanle.submission;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import vn.thanhtuanle.common.enums.SubmissionResult;
import vn.thanhtuanle.entity.Submission;
import vn.thanhtuanle.judge.JudgeService;
import vn.thanhtuanle.language.LanguageRepository;
import vn.thanhtuanle.problem.ProblemRepository;
import vn.thanhtuanle.submission.dto.SubmissionResponseDto;
import vn.thanhtuanle.submission.mapper.SubmissionMapper;
import vn.thanhtuanle.user.UserService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceStreamTest {

    @Mock SubmissionRepository submissionRepository;
    @Mock JudgeService judgeService;
    @Mock ProblemRepository problemRepository;
    @Mock LanguageRepository languageRepository;
    @Mock SubmissionMapper submissionMapper;
    @Mock UserService userService;
    @Mock ApplicationEventPublisher applicationEventPublisher;
    @Mock SubmissionSseRegistry sseRegistry;

    @InjectMocks SubmissionService submissionService;

    @Test
    void streamVerdict_registersEmitterBeforeReadingDb_andReplaysTerminalVerdict() {
        UUID id = UUID.randomUUID();
        Submission s = new Submission();
        s.setId(id);
        s.setStatus(SubmissionResult.ACCEPTED.getValue());
        SubmissionResponseDto dto = SubmissionResponseDto.builder()
                .status(SubmissionResult.ACCEPTED.getValue()).build();
        SseEmitter emitter = new SseEmitter();
        when(sseRegistry.subscribe(id.toString())).thenReturn(emitter);
        when(submissionRepository.findById(id)).thenReturn(Optional.of(s));
        when(submissionMapper.toDto(s)).thenReturn(dto);

        SseEmitter result = submissionService.streamVerdict(id.toString());

        assertThat(result).isSameAs(emitter);
        // Ordering is the race-safety property: subscribe MUST happen before the DB read.
        InOrder inOrder = inOrder(sseRegistry, submissionRepository);
        inOrder.verify(sseRegistry).subscribe(id.toString());
        inOrder.verify(submissionRepository).findById(id);
        verify(sseRegistry).complete(id.toString(), dto);
    }

    @Test
    void streamVerdict_whenPending_doesNotReplay() {
        UUID id = UUID.randomUUID();
        Submission s = new Submission();
        s.setId(id);
        s.setStatus(SubmissionResult.PENDING.getValue());
        when(sseRegistry.subscribe(id.toString())).thenReturn(new SseEmitter());
        when(submissionRepository.findById(id)).thenReturn(Optional.of(s));

        submissionService.streamVerdict(id.toString());

        verify(sseRegistry, never()).complete(any(), any());
        verify(submissionMapper, never()).toDto(any(Submission.class));
    }

    @Test
    void streamVerdict_whenNotFound_completesEmitterWithoutVerdict() {
        UUID id = UUID.randomUUID();
        SseEmitter emitter = mock(SseEmitter.class);
        when(sseRegistry.subscribe(id.toString())).thenReturn(emitter);
        when(submissionRepository.findById(id)).thenReturn(Optional.empty());

        submissionService.streamVerdict(id.toString());

        verify(emitter).complete();
        verify(sseRegistry, never()).complete(any(), any());
    }
}
