package vn.thanhtuanle.submission;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import vn.thanhtuanle.common.enums.SubmissionResult;
import vn.thanhtuanle.entity.Language;
import vn.thanhtuanle.entity.Submission;
import vn.thanhtuanle.language.LanguageRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** "Submissions of problem X" is answered from the submissions alone — no problem row is needed. */
@SpringBootTest
@ActiveProfiles("test")
class SubmissionsByProblemSlugTest {

    @Autowired SubmissionRepository submissionRepository;
    @Autowired LanguageRepository languageRepository;
    @Autowired SubmissionService submissionService;

    private final List<UUID> created = new ArrayList<>();
    private Language language;

    @AfterEach
    void cleanUp() {
        created.forEach(submissionRepository::deleteById);
        if (language != null) {
            languageRepository.deleteById(language.getId());
        }
    }

    private Submission submit(UUID userId, String slug) {
        Submission s = submissionRepository.save(Submission.builder()
                .sourceCode("print(1)").status(SubmissionResult.ACCEPTED.getValue()).time(0).memory(0L)
                .problemId(UUID.randomUUID()).problemSlug(slug).language(language).userId(userId).build());
        created.add(s.getId());
        return s;
    }

    @Test
    void submissionsAreFoundBySlugWithoutAnyProblemRow() {
        language = languageRepository.save(Language.builder()
                .name("slug-lang").identifier("slug-" + UUID.randomUUID()).build());
        String slug = "slug-only-" + UUID.randomUUID();
        UUID user = UUID.randomUUID();
        Submission mine = submit(user, slug);
        submit(UUID.randomUUID(), slug);
        submit(user, "another-problem");

        assertThat(submissionRepository.findByProblemSlugOrderByCreatedAtDesc(slug, PageRequest.of(0, 10)))
                .hasSize(2);
        assertThat(submissionRepository.findByUserIdAndProblemSlugOrderByCreatedAtDesc(user, slug, PageRequest.of(0, 10)))
                .extracting(Submission::getId).containsExactly(mine.getId());
    }

    @Test
    void anUnknownSlugGivesAnEmptyPage() {
        assertThat(submissionService.getSubmissionsByProblemSlug("no-such-" + UUID.randomUUID(), 0, 10).getData())
                .isEmpty();
    }
}
