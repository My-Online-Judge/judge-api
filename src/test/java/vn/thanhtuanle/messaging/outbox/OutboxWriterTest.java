package vn.thanhtuanle.messaging.outbox;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class OutboxWriterTest {

    @Autowired OutboxWriter writer;
    @Autowired OutboxRepository repository;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void aMessageCanOnlyBeWrittenInsideTheTransactionOfTheChangeItAnnounces() {
        assertThatThrownBy(() -> writer.append("t", "k", Map.of("a", 1)))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void thePayloadIsStoredAsTheJsonThatGoesOnTheWire() {
        String key = "writer-" + System.nanoTime();
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> writer.append("t", key, Map.of("a", 1)));

        OutboxMessage stored = repository.findAll().stream()
                .filter(m -> key.equals(m.getMessageKey())).findFirst().orElseThrow();
        assertThat(new String(stored.getPayload(), StandardCharsets.UTF_8)).isEqualTo("{\"a\":1}");
        assertThat(stored.getTopic()).isEqualTo("t");
        assertThat(stored.getPublishedAt()).isNull();
        repository.delete(stored);
    }
}
