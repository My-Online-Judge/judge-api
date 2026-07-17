package vn.thanhtuanle.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
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

    /** Broadcast a verdict to every instance so whichever one holds the SSE emitter delivers it. */
    public void publish(String submissionId, SubmissionResponseDto payload) {
        try {
            String body = objectMapper.writeValueAsString(new VerdictMessage(submissionId, payload));
            redisTemplate.convertAndSend(CHANNEL, body);
        } catch (Exception e) {
            log.error("Failed to publish verdict for submission {}", submissionId, e);
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
