package vn.thanhtuanle.submission.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import org.mapstruct.Mapping;
import vn.thanhtuanle.entity.Submission;
import vn.thanhtuanle.judge.dto.JudgeResultDto;
import vn.thanhtuanle.submission.dto.SubmissionResponseDto;

@Mapper(componentModel = "spring")
public interface SubmissionMapper {
    @Mapping(target = "details", source = "details")
    SubmissionResponseDto toDto(Submission submission, List<JudgeResultDto> details);

    @Mapping(target = "details", ignore = true)
    SubmissionResponseDto toDto(Submission submission);
}
