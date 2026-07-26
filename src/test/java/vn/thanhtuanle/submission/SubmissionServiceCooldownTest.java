package vn.thanhtuanle.submission;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import vn.thanhtuanle.common.exception.ErrorCode;
import vn.thanhtuanle.common.exception.RateLimitedException;
import vn.thanhtuanle.common.exception.ResourceNotFoundException;
import vn.thanhtuanle.entity.Language;
import vn.thanhtuanle.entity.Problem;
import vn.thanhtuanle.entity.Submission;
import vn.thanhtuanle.entity.User;
import vn.thanhtuanle.judge.JudgeService;
import vn.thanhtuanle.language.LanguageRepository;
import vn.thanhtuanle.messaging.event.SubmissionRequestedEvent;
import vn.thanhtuanle.problem.ProblemRepository;
import vn.thanhtuanle.security.SubmissionRateLimiter;
import vn.thanhtuanle.submission.dto.SubmissionRequestDto;
import vn.thanhtuanle.submission.dto.SubmissionResponseDto;
import vn.thanhtuanle.submission.mapper.SubmissionMapper;
import vn.thanhtuanle.user.UserService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubmissionServiceCooldownTest {

    @Mock SubmissionRepository submissionRepository;
    @Mock JudgeService judgeService;
    @Mock ProblemRepository problemRepository;
    @Mock LanguageRepository languageRepository;
    @Mock SubmissionMapper submissionMapper;
    @Mock UserService userService;
    @Mock ApplicationEventPublisher applicationEventPublisher;
    @Mock SubmissionSseRegistry sseRegistry;
    @Mock SubmissionRateLimiter submissionRateLimiter;

    @InjectMocks SubmissionService service;

    private final UUID userId = UUID.randomUUID();
    private SubmissionRequestDto req;

    @BeforeEach
    void setUp() {
        req = new SubmissionRequestDto();
        req.setSourceCode("print(1)");
        req.setLanguageIdentifier("python3");
        req.setProblemSlug("simple-a-plus-b");

        User user = User.builder().username("u").build();
        user.setId(userId);
        when(problemRepository.findByProblemSlug("simple-a-plus-b")).thenReturn(Optional.of(new Problem()));
        when(languageRepository.findByIdentifier("python3")).thenReturn(Optional.of(new Language()));
        when(userService.getCurrentUser()).thenReturn(user);
        when(submissionRepository.save(any())).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });
        when(judgeService.buildRequestedEvent(anyString(), anyString(), any(), any()))
                .thenReturn(SubmissionRequestedEvent.builder().build());
        when(submissionMapper.toDto(any(Submission.class))).thenReturn(new SubmissionResponseDto());
    }

    @Test
    void submit_acquiresCooldown_forTheCurrentUser() {
        assertThatCode(() -> service.submit(req)).doesNotThrowAnyException();
        verify(submissionRateLimiter).acquire(userId);
    }

    @Test
    void unknownProblem_doesNotBurnTheCooldown() {
        when(problemRepository.findByProblemSlug("simple-a-plus-b")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.submit(req)).isInstanceOf(ResourceNotFoundException.class);
        verify(submissionRateLimiter, never()).acquire(any());
    }

    @Test
    void withinCooldown_nothingIsPersisted() {
        doThrow(new RateLimitedException(ErrorCode.SUBMISSION_RATE_LIMITED, 7))
                .when(submissionRateLimiter).acquire(userId);
        assertThatThrownBy(() -> service.submit(req)).isInstanceOf(RateLimitedException.class);
        verify(submissionRepository, never()).save(any());
    }
}
