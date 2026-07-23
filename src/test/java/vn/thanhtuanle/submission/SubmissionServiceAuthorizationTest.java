package vn.thanhtuanle.submission;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceAuthorizationTest {

    @Mock SubmissionRepository submissionRepository;
    @Mock SubmissionMapper submissionMapper;
    @Mock SubmissionDetailAssembler detailAssembler;
    @Mock UserService userService;
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
}
