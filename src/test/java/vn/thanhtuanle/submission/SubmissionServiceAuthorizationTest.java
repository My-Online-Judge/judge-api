package vn.thanhtuanle.submission;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import vn.thanhtuanle.common.exception.ResourceNotFoundException;
import vn.thanhtuanle.entity.Problem;
import vn.thanhtuanle.entity.Submission;
import vn.thanhtuanle.entity.User;
import vn.thanhtuanle.submission.dto.SubmissionResponseDto;
import vn.thanhtuanle.submission.mapper.SubmissionMapper;
import vn.thanhtuanle.user.UserService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceAuthorizationTest {

    @Mock SubmissionRepository submissionRepository;
    @Mock SubmissionMapper submissionMapper;
    @Mock SubmissionDetailAssembler detailAssembler;
    @Mock UserService userService;
    @Mock SubmissionSseRegistry sseRegistry;
    @InjectMocks SubmissionService service;

    private User user(UUID id) {
        User u = new User();
        u.setId(id);
        return u;
    }

    private Submission ownedBy(UUID submissionId, User owner) {
        Submission s = Submission.builder().problem(new Problem()).user(owner).build();
        s.setId(submissionId);
        return s;
    }

    @Test
    void owner_canRead() {
        UUID ownerId = UUID.randomUUID();
        UUID subId = UUID.randomUUID();
        User owner = user(ownerId);
        Submission s = ownedBy(subId, owner);

        when(submissionRepository.findById(subId)).thenReturn(Optional.of(s));
        when(userService.getCurrentUser()).thenReturn(owner);
        when(submissionMapper.toDto(eq(s), any()))
                .thenReturn(SubmissionResponseDto.builder().build());

        assertThat(service.getSubmissionById(subId.toString())).isNotNull();
    }

    @Test
    void nonOwner_gets404NotForbidden() {
        UUID subId = UUID.randomUUID();
        Submission s = ownedBy(subId, user(UUID.randomUUID()));

        when(submissionRepository.findById(subId)).thenReturn(Optional.of(s));
        when(userService.getCurrentUser()).thenReturn(user(UUID.randomUUID()));

        assertThatThrownBy(() -> service.getSubmissionById(subId.toString()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void streamVerdict_nonOwnerWithoutReadAny_deniedAndRegistryNeverTouched() {
        UUID subId = UUID.randomUUID();
        Submission s = ownedBy(subId, user(UUID.randomUUID()));

        when(submissionRepository.findById(subId)).thenReturn(Optional.of(s));
        when(userService.getCurrentUser()).thenReturn(user(UUID.randomUUID()));

        assertThatThrownBy(() -> service.streamVerdict(subId.toString()))
                .isInstanceOf(ResourceNotFoundException.class);

        // No emitter may ever be registered for a request that fails the read check —
        // otherwise it would evict a legitimate owner's live emitter (eviction DoS).
        verifyNoInteractions(sseRegistry);
    }

    @Test
    void streamVerdict_unknownId_throwsNotFound() {
        UUID subId = UUID.randomUUID();
        when(submissionRepository.findById(subId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.streamVerdict(subId.toString()))
                .isInstanceOf(ResourceNotFoundException.class);

        // Unknown id must behave identically to a denied id (both 404) so existence can't
        // be inferred from the response shape.
        verifyNoInteractions(sseRegistry);
    }

    @Test
    void getSubmissionsByUser_otherUserWithoutReadAny_deniedAndRepositoryNeverQueried() {
        UUID requestedUserId = UUID.randomUUID();
        when(userService.getCurrentUser()).thenReturn(user(UUID.randomUUID()));

        assertThatThrownBy(() -> service.getSubmissionsByUser(requestedUserId.toString(), 0, 10))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(submissionRepository, never()).findByUserIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void getSubmissionsByUser_ownId_succeeds() {
        UUID userId = UUID.randomUUID();
        when(userService.getCurrentUser()).thenReturn(user(userId));
        when(submissionRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any()))
                .thenReturn(Page.empty());

        assertThat(service.getSubmissionsByUser(userId.toString(), 0, 10)).isNotNull();
    }

    @Test
    void getSubmissionsByUserAndProblem_otherUserWithoutReadAny_deniedAndRepositoryNeverQueried() {
        UUID requestedUserId = UUID.randomUUID();
        when(userService.getCurrentUser()).thenReturn(user(UUID.randomUUID()));

        assertThatThrownBy(() -> service.getSubmissionsByUserAndProblem(requestedUserId.toString(), "two-sum", 0, 10))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(submissionRepository, never()).findByUserIdAndProblemSlugOrderByCreatedAtDesc(any(), any(), any());
    }

    @Test
    void getSubmissionsByUserAndProblem_ownId_succeeds() {
        UUID userId = UUID.randomUUID();
        when(userService.getCurrentUser()).thenReturn(user(userId));
        when(submissionRepository.findByUserIdAndProblemSlugOrderByCreatedAtDesc(eq(userId), eq("two-sum"), any()))
                .thenReturn(Page.empty());

        assertThat(service.getSubmissionsByUserAndProblem(userId.toString(), "two-sum", 0, 10)).isNotNull();
    }
}
