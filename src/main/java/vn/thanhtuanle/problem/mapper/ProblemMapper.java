package vn.thanhtuanle.problem.mapper;

import org.mapstruct.Mapper;

import vn.thanhtuanle.common.enums.ProblemStatus;
import vn.thanhtuanle.entity.Problem;
import vn.thanhtuanle.problem.dto.ProblemResponseDto;

@Mapper(componentModel = "spring")
public interface ProblemMapper {
    ProblemResponseDto toDto(Problem problem);

    default ProblemStatus map(int value) {
        return ProblemStatus.fromValue(value);
    }

    default int map(ProblemStatus status) {
        return status != null ? status.getValue() : 0;
    }
}
