package vn.thanhtuanle.submission.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.thanhtuanle.entity.Submission;
import vn.thanhtuanle.submission.dto.SubmissionResponseDto;
import vn.thanhtuanle.submission.dto.TestCaseResultDto;

@Mapper(componentModel = "spring")
public interface SubmissionMapper {

    /** Full view: per-testcase details already filtered by SubmissionDetailAssembler. */
    @Mapping(target = "details", source = "details")
    SubmissionResponseDto toDto(Submission submission, List<TestCaseResultDto> details);

    /** List view: details deliberately omitted, see SubmissionService. */
    @Mapping(target = "details", ignore = true)
    SubmissionResponseDto toDto(Submission submission);
}
