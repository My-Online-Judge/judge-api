package vn.thanhtuanle.problem;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import vn.thanhtuanle.submission.problem.ProblemCatalog;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

/**
 * {@code sampleCases} runs inside the caller's transaction — the verdict transaction of
 * {@code JudgeResultConsumer} among them. Failing closed must mean "rows shown hidden", never "the
 * caller's transaction is marked rollback-only and the verdict is lost".
 */
@SpringBootTest
@ActiveProfiles("test")
class LocalProblemCatalogTransactionTest {

    @Autowired ProblemCatalog catalog;
    @Autowired PlatformTransactionManager transactionManager;
    // A mock has no transactional proxy of its own: the failure surfaces inside TestCaseService.
    @MockitoBean ProblemRepository problemRepository;

    @Test
    void aFailureReadingSamplesLeavesTheCallersTransactionCommittable() {
        UUID problemId = UUID.randomUUID();
        when(problemRepository.findById(problemId)).thenThrow(new IllegalStateException("unexpected"));

        assertThatCode(() -> new TransactionTemplate(transactionManager).executeWithoutResult(tx ->
                assertThat(catalog.sampleCases(problemId)).isEmpty()))
                .doesNotThrowAnyException();
    }
}
