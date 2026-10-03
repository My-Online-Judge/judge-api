package vn.thanhtuanle.submission;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import vn.thanhtuanle.common.enums.SubmissionResult;
import vn.thanhtuanle.entity.Language;
import vn.thanhtuanle.entity.Submission;
import vn.thanhtuanle.language.LanguageRepository;
import vn.thanhtuanle.messaging.KafkaTopics;
import vn.thanhtuanle.messaging.outbox.OutboxMessage;
import vn.thanhtuanle.messaging.outbox.OutboxRepository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A submission is stuck only when its judging should have finished: its judge request left the outbox
 * long enough ago. While Kafka is down the request waits in the outbox, and flipping the submission to
 * SYSTEM_ERROR then would discard the real verdict that arrives once Kafka is back.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SubmissionStuckQueryTest {

    @Autowired SubmissionRepository submissionRepository;
    @Autowired OutboxRepository outboxRepository;
    @Autowired LanguageRepository languageRepository;
    @Autowired JdbcTemplate jdbc;

    private final LocalDateTime now = LocalDateTime.now();
    private final LocalDateTime threshold = now.minusMinutes(5);

    private UUID pending(Language language, LocalDateTime createdAt) {
        Submission s = submissionRepository.saveAndFlush(Submission.builder()
                .sourceCode("x").status(SubmissionResult.PENDING.getValue()).time(0).memory(0L)
                .problemId(UUID.randomUUID()).problemSlug("p").language(language).userId(UUID.randomUUID()).build());
        jdbc.update("UPDATE t_submissions SET created_at = ? WHERE id = ?", Timestamp.valueOf(createdAt), s.getId());
        return s.getId();
    }

    private void request(UUID submissionId, LocalDateTime publishedAt) {
        outboxRepository.saveAndFlush(OutboxMessage.builder().topic(KafkaTopics.SUBMISSION_REQUESTED)
                .messageKey(submissionId.toString()).payload(new byte[] {1})
                .createdAt(now.minusMinutes(10)).publishedAt(publishedAt).build());
    }

    @Test
    void onlySubmissionsWhoseRequestLeftTheOutboxLongAgoAreStuck() {
        Language language = languageRepository.save(Language.builder()
                .name("stuck-lang").identifier("stuck-" + UUID.randomUUID()).build());

        UUID waitingForKafka = pending(language, now.minusMinutes(10));
        request(waitingForKafka, null);
        UUID justSent = pending(language, now.minusMinutes(10));
        request(justSent, now.minusMinutes(1));
        UUID sentLongAgo = pending(language, now.minusMinutes(10));
        request(sentLongAgo, now.minusMinutes(9));
        UUID noOutboxRow = pending(language, now.minusMinutes(10));   // sent before 2a, or its row was cleaned up
        UUID young = pending(language, now.minusMinutes(1));
        List<UUID> mine = List.of(waitingForKafka, justSent, sentLongAgo, noOutboxRow, young);

        List<UUID> stuck = submissionRepository.findStuck(
                List.of(SubmissionResult.PENDING.getValue(), SubmissionResult.JUDGING.getValue()), threshold)
                .stream().map(Submission::getId).filter(mine::contains).toList();

        assertThat(stuck).containsExactlyInAnyOrder(sentLongAgo, noOutboxRow);
    }
}
