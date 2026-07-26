package vn.thanhtuanle.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.thanhtuanle.submission.SubmissionSseRegistry;
import vn.thanhtuanle.submission.dto.SubmissionResponseDto;

/**
 * Fans out submission verdicts across judge-api instances so an SSE subscriber is notified no
 * matter which instance consumed the Kafka verdict.
 *
 * <p>The DB write stays single (JudgeResultConsumer, shared group, idempotent). After it commits,
 * the verdict is PUBLISHed to the {@code oj.verdicts} Redis channel; every instance SUBSCRIBEs and,
 * on receipt, pushes to its own local {@link SubmissionSseRegistry} — a no-op unless that instance
 * holds the submission's emitter. This decouples "which instance consumed the Kafka message" from
 * "which instance holds the SSE connection".
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VerdictPubSub implements MessageListener {

    public static final String CHANNEL = "oj.verdicts";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SubmissionSseRegistry sseRegistry;

    /**
     * Broadcast a verdict to every instance so whichever one holds the SSE emitter delivers it.
     *
     * <p>Package-private on purpose: every publish that follows a state write MUST go through
     * {@link #publishAfterCommit} instead, and this visibility makes that a compile error for
     * any caller outside this package rather than a convention someone can forget (as
     * {@code SubmissionReconcileJob} once did). Direct callers within this package (this class,
     * and this package's own tests) are the deferral mechanism itself and the no-transaction
     * immediate-publish path — never a second, competing writer.
     */
    void publish(String submissionId, SubmissionResponseDto payload) {
        try {
            String body = objectMapper.writeValueAsString(new VerdictMessage(submissionId, payload));
            redisTemplate.convertAndSend(CHANNEL, body);
        } catch (Exception e) {
            log.error("Failed to publish verdict for submission {}", submissionId, e);
        }
    }

    /**
     * Publish a verdict only AFTER the caller's transaction commits — or immediately when no
     * transaction synchronization is active (direct calls, unit tests). Publishing mid-transaction
     * opens a lost-verdict window: a subscriber's fresh status re-read
     * ({@code SubmissionService.streamVerdict}) can still see the row as uncommitted-PENDING while
     * the live publish has already passed its emitter by. Post-commit, a subscriber either
     * registered before the publish (receives it live) or re-reads after the commit (sees the
     * terminal row, replays). Every publish that follows a state write MUST go through this method,
     * never through {@link #publish} directly — and since {@link #publish} is package-private,
     * that rule is enforced by the compiler for every caller outside this package, not left to
     * convention or review.
     */
    public void publishAfterCommit(String submissionId, SubmissionResponseDto payload) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish(submissionId, payload);
                }
            });
        } else {
            publish(submissionId, payload);
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            VerdictMessage msg = objectMapper.readValue(message.getBody(), VerdictMessage.class);
            sseRegistry.complete(msg.submissionId(), msg.payload());
        } catch (Exception e) {
            log.error("Failed to handle verdict pub/sub message", e);
        }
    }

    public record VerdictMessage(String submissionId, SubmissionResponseDto payload) {}
}
