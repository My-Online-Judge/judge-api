package vn.thanhtuanle.messaging.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.thanhtuanle.common.util.AfterCommit;

import java.time.LocalDateTime;

/**
 * Queues a Kafka record inside the caller's transaction: it exists if and only if the change it
 * announces was committed. After the commit the relay is asked to send it at once.
 */
@Component
@RequiredArgsConstructor
public class OutboxWriter {

    private final OutboxRepository repository;
    private final ObjectMapper objectMapper;
    private final OutboxRelay relay;

    @Transactional(propagation = Propagation.MANDATORY)
    public void append(String topic, String key, Object payload) {
        byte[] json;
        try {
            json = objectMapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize a " + payload.getClass().getSimpleName(), e);
        }
        repository.save(OutboxMessage.builder()
                .topic(topic).messageKey(key).payload(json).createdAt(LocalDateTime.now()).build());
        AfterCommit.run(relay::relaySoon);
    }
}
