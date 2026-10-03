package vn.thanhtuanle.messaging.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The relay against real Postgres (V17 schema, {@code FOR UPDATE SKIP LOCKED}): every committed row is
 * sent, oldest first, a failure leaves the row for the next pass, and two relays never send the same row.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@Transactional(propagation = Propagation.NOT_SUPPORTED)   // each relay pass commits its own transaction
class OutboxRelayPostgresTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired OutboxRepository repository;
    @Autowired PlatformTransactionManager transactionManager;

    private final List<String> sent = Collections.synchronizedList(new ArrayList<>());

    @BeforeEach
    void emptyOutbox() {
        repository.deleteAll();
        sent.clear();
    }

    private OutboxRelay relay(OutboxSender sender, int batchSize) {
        OutboxProperties props = new OutboxProperties();
        props.setBatchSize(batchSize);
        return new OutboxRelay(repository, sender, transactionManager, props);
    }

    private void append(String key, int secondsAgo) {
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> repository.save(OutboxMessage.builder()
                .topic("t").messageKey(key).payload(key.getBytes(StandardCharsets.UTF_8))
                .createdAt(LocalDateTime.now().minusSeconds(secondsAgo)).build()));
    }

    @Test
    void sendsOldestFirstAndMarksEachRowPublished() {
        append("second", 20);
        append("third", 10);
        append("first", 30);

        int count = relay(m -> sent.add(m.getMessageKey()), 100).relayBatch();

        assertThat(count).isEqualTo(3);
        assertThat(sent).containsExactly("first", "second", "third");
        assertThat(repository.countByPublishedAtIsNull()).isZero();
    }

    @Test
    void aFailedSendLeavesThatRowAndTheLaterOnesForTheNextPass() {
        append("a", 30);
        append("b", 20);
        append("c", 10);
        OutboxSender kafkaDownAtB = m -> {
            if (m.getMessageKey().equals("b")) {
                throw new IllegalStateException("broker down");
            }
            sent.add(m.getMessageKey());
        };

        assertThat(relay(kafkaDownAtB, 100).relayBatch()).isEqualTo(1);
        assertThat(repository.countByPublishedAtIsNull()).isEqualTo(2);

        relay(m -> sent.add(m.getMessageKey()), 100).relayBatch();   // Kafka is back
        assertThat(sent).containsExactly("a", "b", "c");
        assertThat(repository.countByPublishedAtIsNull()).isZero();
    }

    @Test
    void twoRelaysNeverSendTheSameRow() throws Exception {
        for (int i = 0; i < 20; i++) {
            append("m" + i, 100 - i);
        }
        CountDownLatch bothSending = new CountDownLatch(2);
        List<Boolean> overlapped = Collections.synchronizedList(new ArrayList<>());
        OutboxSender slow = m -> {
            bothSending.countDown();
            // Holds this relay's row locks until the other relay is sending too. With plain FOR UPDATE the
            // other relay would wait on these locks instead, and this would time out.
            overlapped.add(bothSending.await(5, TimeUnit.SECONDS));
            sent.add(m.getMessageKey());
        };
        ExecutorService twoInstances = Executors.newFixedThreadPool(2);
        Future<Integer> one = twoInstances.submit(() -> relay(slow, 5).relayBatch());
        Future<Integer> two = twoInstances.submit(() -> relay(slow, 5).relayBatch());

        assertThat(one.get(20, TimeUnit.SECONDS) + two.get(20, TimeUnit.SECONDS)).isEqualTo(10);
        assertThat(sent).hasSize(10).doesNotHaveDuplicates();
        assertThat(overlapped).as("both relays held disjoint rows at the same time").containsOnly(true);
        twoInstances.shutdown();
    }

    @Test
    void theCleanUpDeletesOnlyOldPublishedRows() {
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            repository.save(OutboxMessage.builder().topic("t").messageKey("old").payload(new byte[] {1})
                    .createdAt(LocalDateTime.now().minusDays(9)).publishedAt(LocalDateTime.now().minusDays(8)).build());
            repository.save(OutboxMessage.builder().topic("t").messageKey("recent").payload(new byte[] {1})
                    .createdAt(LocalDateTime.now().minusDays(1)).publishedAt(LocalDateTime.now().minusDays(1)).build());
            repository.save(OutboxMessage.builder().topic("t").messageKey("unsent").payload(new byte[] {1})
                    .createdAt(LocalDateTime.now().minusDays(9)).build());
        });

        relay(m -> { }, 100).deletePublished();

        assertThat(repository.findAll()).extracting(OutboxMessage::getMessageKey)
                .containsExactlyInAnyOrder("recent", "unsent");
    }
}
