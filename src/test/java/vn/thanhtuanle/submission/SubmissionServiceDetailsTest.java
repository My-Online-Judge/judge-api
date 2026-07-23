package vn.thanhtuanle.submission;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.thanhtuanle.entity.Problem;
import vn.thanhtuanle.entity.Submission;
import vn.thanhtuanle.entity.User;
import vn.thanhtuanle.submission.dto.SubmissionResponseDto;
import vn.thanhtuanle.submission.dto.TestCaseResultDto;
import vn.thanhtuanle.submission.mapper.SubmissionMapper;
import vn.thanhtuanle.user.UserService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceDetailsTest {

    @Mock SubmissionRepository submissionRepository;
    @Mock SubmissionMapper submissionMapper;
    @Mock SubmissionDetailAssembler detailAssembler;
    @Mock UserService userService;
    @InjectMocks SubmissionService service;

    @Test
    void getById_populatesDetailsFromAssembler() {
        UUID id = UUID.randomUUID();
        User owner = new User();
        owner.setId(UUID.randomUUID());
        Submission s = Submission.builder().problem(new Problem()).user(owner).build();
        s.setId(id);
        when(submissionRepository.findById(id)).thenReturn(Optional.of(s));
        when(userService.getCurrentUser()).thenReturn(owner);

        List<TestCaseResultDto> rows = List.of(
                TestCaseResultDto.builder().name("1").result(0).build());
        when(detailAssembler.assemble(s)).thenReturn(rows);
        when(submissionMapper.toDto(eq(s), any()))
                .thenReturn(SubmissionResponseDto.builder().details(rows).build());

        SubmissionResponseDto dto = service.getSubmissionById(id.toString());

        assertThat(dto.getDetails()).hasSize(1);
        assertThat(dto.getDetails().get(0).getName()).isEqualTo("1");
    }
}
