package vn.thanhtuanle.submission;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import vn.thanhtuanle.common.enums.SubmissionResult;
import vn.thanhtuanle.entity.Language;
import vn.thanhtuanle.entity.Problem;
import vn.thanhtuanle.entity.Submission;
import vn.thanhtuanle.entity.User;
import vn.thanhtuanle.language.LanguageRepository;
import vn.thanhtuanle.problem.ProblemRepository;
import vn.thanhtuanle.user.UserRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the Hibernate first-level-cache semantics that make the scalar
 * {@link SubmissionRepository#findStatusById} load-bearing in
 * {@code SubmissionService.streamVerdict} — semantics a Mockito test cannot express:
 *
 * Within one open persistence context, a repeated {@code findById} does NOT re-read the
 * database: it returns the already-managed instance, so a status committed by another
 * transaction in the meantime is invisible to it. A JPQL scalar select, by contrast,
 * always hits the database and returns the committed value directly, bypassing the
 * persistence context. {@code EntityManager.refresh} then forces a DB re-read into the
 * managed instance. This is exactly the sequence streamVerdict relies on.
 */
@SpringBootTest
@ActiveProfiles("test")
class SubmissionRepositoryFreshReadTest {

    @Autowired SubmissionRepository submissionRepository;
    @Autowired ProblemRepository problemRepository;
    @Autowired LanguageRepository languageRepository;
    @Autowired UserRepository userRepository;
    @Autowired PlatformTransactionManager transactionManager;
    // Shared transactional proxy: inside a TransactionTemplate callback it delegates to the
    // persistence context bound to that transaction.
    @Autowired EntityManager entityManager;

    private UUID submissionId;
    private UUID problemId;
    private UUID languageId;
    private UUID userId;

    @BeforeEach
    void createPendingSubmission() {
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            User user = userRepository.save(User.builder()
                    .username("fresh-read-user-" + UUID.randomUUID()).build());
            Problem problem = problemRepository.save(Problem.builder()
                    .title("fresh-read-problem").problemSlug("fresh-read-" + UUID.randomUUID()).build());
            Language language = languageRepository.save(Language.builder()
                    .name("fresh-read-lang").identifier("fresh-read-" + UUID.randomUUID()).build());
            Submission submission = submissionRepository.save(Submission.builder()
                    .sourceCode("print(1)")
                    .status(SubmissionResult.PENDING.getValue())
                    .time(0).memory(0L)
                    .problem(problem).language(language).user(user)
                    .build());
            userId = user.getId();
            problemId = problem.getId();
            languageId = language.getId();
            submissionId = submission.getId();
        });
    }

    @AfterEach
    void cleanUp() {
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            submissionRepository.deleteById(submissionId);
            problemRepository.deleteById(problemId);
            languageRepository.deleteById(languageId);
            userRepository.deleteById(userId);
        });
    }

    @Test
    void scalarStatusQuerySeesCommittedUpdate_whileFindByIdServesStaleManagedInstance() {
        // Reader transaction mirrors streamVerdict's @Transactional(readOnly = true).
        TransactionTemplate readerTx = new TransactionTemplate(transactionManager);
        readerTx.setReadOnly(true);

        readerTx.executeWithoutResult(tx -> {
            // tx1: load the submission — it becomes managed in tx1's persistence context.
            Submission managed = submissionRepository.findById(submissionId).orElseThrow();
            assertThat(managed.getStatus()).isEqualTo(SubmissionResult.PENDING.getValue());

            // tx2 (separate, committed while tx1 is still open): the verdict lands.
            TransactionTemplate writerTx = new TransactionTemplate(transactionManager);
            writerTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            writerTx.executeWithoutResult(tx2 -> {
                Submission other = submissionRepository.findById(submissionId).orElseThrow();
                other.setStatus(SubmissionResult.ACCEPTED.getValue());
                submissionRepository.save(other);
            });

            // Back in tx1: findById is a no-op re-read — it serves the SAME stale managed
            // instance from the first-level cache without touching the database. This is why
            // streamVerdict's old "fresh" re-read could never see the verdict.
            Submission reread = submissionRepository.findById(submissionId).orElseThrow();
            assertThat(reread).isSameAs(managed);
            assertThat(reread.getStatus())
                    .as("findById within the same persistence context must serve the stale L1 copy")
                    .isEqualTo(SubmissionResult.PENDING.getValue());

            // The scalar projection bypasses the persistence context and sees tx2's commit.
            assertThat(submissionRepository.findStatusById(submissionId))
                    .as("scalar JPQL select must hit the database and see the committed value")
                    .isEqualTo(SubmissionResult.ACCEPTED.getValue());

            // refresh() forces a DB re-read into the managed instance — the replay payload
            // streamVerdict maps after the scalar check is therefore the committed state.
            entityManager.refresh(managed);
            assertThat(managed.getStatus()).isEqualTo(SubmissionResult.ACCEPTED.getValue());
        });
    }
}
