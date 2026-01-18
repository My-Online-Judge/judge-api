package vn.thanhtuanle.language;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.thanhtuanle.entity.Language;
import vn.thanhtuanle.language.dto.LanguageResponseDto;
import vn.thanhtuanle.language.mapper.LanguageMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LanguageService {

    private final LanguageRepository languageRepository;
    private final LanguageMapper languageMapper;

    @Transactional(readOnly = true)
    public List<LanguageResponseDto> getAllLanguages() {
        log.info("Fetching all active programming languages");

        return languageRepository.findAllByIsDisabledFalseOrderByNameAsc()
                .stream()
                .map(languageMapper::toResponseDto)
                .toList();
    }

}
