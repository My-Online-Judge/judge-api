package vn.thanhtuanle.language;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.thanhtuanle.entity.Language;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LanguageRepository extends JpaRepository<Language, UUID> {

    Optional<Language> findByIdentifier(String identifier);
}
