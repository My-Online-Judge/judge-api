package vn.thanhtuanle.messaging.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxMessage, UUID> {

    /**
     * Locks the oldest unpublished rows for the caller's transaction. {@code SKIP LOCKED}: rows another
     * relay (another instance) holds are skipped, so two instances never send the same batch.
     */
    @Query(value = "SELECT * FROM t_outbox WHERE published_at IS NULL ORDER BY created_at LIMIT :limit "
            + "FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxMessage> lockOldestUnpublished(@Param("limit") int limit);

    long countByPublishedAtIsNull();

    @Query("SELECT min(o.createdAt) FROM OutboxMessage o WHERE o.publishedAt IS NULL")
    LocalDateTime oldestUnpublishedCreatedAt();

    @Modifying
    @Query("DELETE FROM OutboxMessage o WHERE o.publishedAt < :before")
    int deletePublishedBefore(@Param("before") LocalDateTime before);
}
