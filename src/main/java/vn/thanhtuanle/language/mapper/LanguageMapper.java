package vn.thanhtuanle.language.mapper;

import org.mapstruct.Mapper;
import vn.thanhtuanle.entity.Language;
import vn.thanhtuanle.language.dto.LanguageResponseDto;

@Mapper(componentModel = "spring")
public interface LanguageMapper {

    LanguageResponseDto toResponseDto(Language language);
}
