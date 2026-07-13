package vn.thanhtuanle.submission;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.context.ApplicationEventPublisher;

import vn.thanhtuanle.common.enums.SubmissionResult;
import vn.thanhtuanle.entity.Language;
import vn.thanhtuanle.entity.Problem;
import vn.thanhtuanle.entity.Submission;
import vn.thanhtuanle.entity.User;
import vn.thanhtuanle.judge.JudgeService;
import vn.thanhtuanle.language.LanguageRepository;
import vn.thanhtuanle.messaging.event.SubmissionRequestedAppEvent;
import vn.thanhtuanle.messaging.event.SubmissionRequestedEvent;
import vn.thanhtuanle.problem.ProblemRepository;
import vn.thanhtuanle.submission.dto.SubmissionRequestDto;
import vn.thanhtuanle.submission.dto.SubmissionResponseDto;
import vn.thanhtuanle.submission.mapper.SubmissionMapper;
import vn.thanhtuanle.user.UserService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceSubmitTest {

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
    void submit_savesPending_publishesEvent_andReturnsPending() {
        SubmissionRequestDto req = SubmissionRequestDto.builder()
                .sourceCode("int main(){}").languageIdentifier("cpp")
                .problemSlug("a-plus-b").shareSubmission(false).build();

        Problem problem = new Problem();
        Language language = new Language();
        User user = new User();

        when(problemRepository.findByProblemSlug("a-plus-b")).thenReturn(Optional.of(problem));
        when(languageRepository.findByIdentifier("cpp")).thenReturn(Optional.of(language));
        when(userService.getCurrentUser()).thenReturn(user);
        // simulate JPA's GenerationType.UUID assigning an id on save(), since a bare Mockito
        // mock does not run Hibernate's identifier-generation logic.
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });
        when(judgeService.buildRequestedEvent(any(), any(), any(), any()))
                .thenReturn(SubmissionRequestedEvent.builder().submissionId("x").build());
        when(submissionMapper.toDto(any(Submission.class)))
                .thenReturn(SubmissionResponseDto.builder()
                        .status(SubmissionResult.PENDING.getValue()).build());

        SubmissionResponseDto dto = submissionService.submit(req);

        verify(judgeService).validate("int main(){}");
        ArgumentCaptor<Submission> saved = ArgumentCaptor.forClass(Submission.class);
        verify(submissionRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(SubmissionResult.PENDING.getValue());
        verify(applicationEventPublisher).publishEvent(any(SubmissionRequestedAppEvent.class));
        assertThat(dto.getStatus()).isEqualTo(SubmissionResult.PENDING.getValue());
    }
}
