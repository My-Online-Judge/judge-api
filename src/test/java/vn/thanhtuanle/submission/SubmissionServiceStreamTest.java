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
import vn.thanhtuanle.common.exception.ResourceNotFoundException;
import vn.thanhtuanle.entity.Submission;
import vn.thanhtuanle.entity.User;
import vn.thanhtuanle.judge.JudgeService;
import vn.thanhtuanle.language.LanguageRepository;
import vn.thanhtuanle.problem.ProblemRepository;
import vn.thanhtuanle.submission.dto.SubmissionResponseDto;
import vn.thanhtuanle.submission.mapper.SubmissionMapper;
import vn.thanhtuanle.user.UserService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    @Mock SubmissionDetailAssembler detailAssembler;

    @InjectMocks SubmissionService submissionService;

    @Test
    void streamVerdict_loadsAndAuthorizesBeforeSubscribing_thenReplaysTerminalVerdict() {
        UUID id = UUID.randomUUID();
        User owner = new User();
        owner.setId(UUID.randomUUID());
        Submission s = new Submission();
        s.setId(id);
        s.setUser(owner);
        s.setStatus(SubmissionResult.ACCEPTED.getValue());
        SubmissionResponseDto dto = SubmissionResponseDto.builder()
                .status(SubmissionResult.ACCEPTED.getValue()).build();
        SseEmitter emitter = new SseEmitter();
        when(submissionRepository.findById(id)).thenReturn(Optional.of(s));
        when(userService.getCurrentUser()).thenReturn(owner);
        when(sseRegistry.subscribe(id.toString())).thenReturn(emitter);
        when(submissionMapper.toDto(eq(s), any())).thenReturn(dto);

        SseEmitter result = submissionService.streamVerdict(id.toString());

        assertThat(result).isSameAs(emitter);
        // Ordering is the security property: load + authorize MUST happen before subscribe,
        // so a denied/unknown request never registers (and evicts) an emitter.
        InOrder inOrder = inOrder(submissionRepository, sseRegistry);
        inOrder.verify(submissionRepository).findById(id);
        inOrder.verify(sseRegistry).subscribe(id.toString());
        verify(sseRegistry).complete(id.toString(), dto);
    }

    @Test
    void streamVerdict_whenPending_doesNotReplay() {
        UUID id = UUID.randomUUID();
        User owner = new User();
        owner.setId(UUID.randomUUID());
        Submission s = new Submission();
        s.setId(id);
        s.setUser(owner);
        s.setStatus(SubmissionResult.PENDING.getValue());
        when(submissionRepository.findById(id)).thenReturn(Optional.of(s));
        when(userService.getCurrentUser()).thenReturn(owner);
        when(sseRegistry.subscribe(id.toString())).thenReturn(new SseEmitter());

        submissionService.streamVerdict(id.toString());

        verify(sseRegistry, never()).complete(any(), any());
        verify(submissionMapper, never()).toDto(any(Submission.class));
    }

    @Test
    void streamVerdict_whenJudging_doesNotReplay() {
        UUID id = UUID.randomUUID();
        User owner = new User();
        owner.setId(UUID.randomUUID());
        Submission s = new Submission();
        s.setId(id);
        s.setUser(owner);
        s.setStatus(SubmissionResult.JUDGING.getValue());
        when(submissionRepository.findById(id)).thenReturn(Optional.of(s));
        when(userService.getCurrentUser()).thenReturn(owner);
        when(sseRegistry.subscribe(id.toString())).thenReturn(new SseEmitter());

        submissionService.streamVerdict(id.toString());

        verify(sseRegistry, never()).complete(any(), any());
        verify(submissionMapper, never()).toDto(any(Submission.class));
    }

    @Test
    void streamVerdict_verdictArrivesBetweenLoadAndSubscribe_replaysFromFreshRead() {
        // Pins the missed-verdict race: the terminal replay decision must be made from a
        // read taken AFTER subscribe, not the pre-subscribe snapshot used to authorize.
        UUID id = UUID.randomUUID();
        User owner = new User();
        owner.setId(UUID.randomUUID());

        Submission pendingAtLoad = new Submission();
        pendingAtLoad.setId(id);
        pendingAtLoad.setUser(owner);
        pendingAtLoad.setStatus(SubmissionResult.PENDING.getValue());

        Submission terminalAtSubscribe = new Submission();
        terminalAtSubscribe.setId(id);
        terminalAtSubscribe.setUser(owner);
        terminalAtSubscribe.setStatus(SubmissionResult.ACCEPTED.getValue());

        SubmissionResponseDto dto = SubmissionResponseDto.builder()
                .status(SubmissionResult.ACCEPTED.getValue()).build();
        SseEmitter emitter = new SseEmitter();

        // Consecutive stubbing: the verdict "lands" between the authorizing read and subscribe.
        when(submissionRepository.findById(id)).thenReturn(Optional.of(pendingAtLoad), Optional.of(terminalAtSubscribe));
        when(userService.getCurrentUser()).thenReturn(owner);
        when(sseRegistry.subscribe(id.toString())).thenReturn(emitter);
        when(submissionMapper.toDto(eq(terminalAtSubscribe), any())).thenReturn(dto);

        submissionService.streamVerdict(id.toString());

        verify(sseRegistry).complete(id.toString(), dto);
    }

    @Test
    void streamVerdict_whenNotFound_throwsResourceNotFound() {
        // Behavior change (approved): unknown id used to complete an already-registered
        // emitter with no verdict (200-empty). It now 404s before ever subscribing, so an
        // unknown id is indistinguishable from a denied id (closes the id-existence oracle).
        UUID id = UUID.randomUUID();
        when(submissionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.streamVerdict(id.toString()))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(sseRegistry);
    }
}
