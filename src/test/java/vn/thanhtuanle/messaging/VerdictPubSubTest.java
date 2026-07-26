package vn.thanhtuanle.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.thanhtuanle.common.enums.SubmissionResult;
import vn.thanhtuanle.submission.SubmissionSseRegistry;
import vn.thanhtuanle.submission.dto.SubmissionResponseDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerdictPubSubTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock SubmissionSseRegistry sseRegistry;
    final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private VerdictPubSub pubSub() {
        return new VerdictPubSub(redisTemplate, objectMapper, sseRegistry);
    }

    @Test
    void publish_sendsJsonToChannel() {
        SubmissionResponseDto dto = SubmissionResponseDto.builder()
                .status(SubmissionResult.ACCEPTED.getValue()).build();

        pubSub().publish("sub-1", dto);

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(eq(VerdictPubSub.CHANNEL), body.capture());
        assertThat(body.getValue()).contains("sub-1");
    }

    @Test
    void onMessage_deserializesAndPushesToLocalRegistry() throws Exception {
        SubmissionResponseDto dto = SubmissionResponseDto.builder()
                .status(SubmissionResult.SYSTEM_ERROR.getValue()).build();
        String json = objectMapper.writeValueAsString(
                new VerdictPubSub.VerdictMessage("sub-2", dto));
        Message message = mock(Message.class);
        when(message.getBody()).thenReturn(json.getBytes());

        pubSub().onMessage(message, null);

        ArgumentCaptor<SubmissionResponseDto> pushed =
                ArgumentCaptor.forClass(SubmissionResponseDto.class);
        verify(sseRegistry).complete(eq("sub-2"), pushed.capture());
        assertThat(pushed.getValue().getStatus()).isEqualTo(SubmissionResult.SYSTEM_ERROR.getValue());
    }

    @Test
    void publishAfterCommit_withActiveTransaction_defersUntilAfterCommit() {
        SubmissionResponseDto dto = SubmissionResponseDto.builder()
                .status(SubmissionResult.ACCEPTED.getValue()).build();

        TransactionSynchronizationManager.initSynchronization();
        try {
            pubSub().publishAfterCommit("sub-4", dto);

            // The transaction has not committed: nothing may reach the wire yet.
            verify(redisTemplate, never()).convertAndSend(any(), any());

            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCommit();
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(eq(VerdictPubSub.CHANNEL), body.capture());
        assertThat(body.getValue()).contains("sub-4");
    }

    @Test
    void publishAfterCommit_withoutTransaction_publishesImmediately() {
        SubmissionResponseDto dto = SubmissionResponseDto.builder()
                .status(SubmissionResult.ACCEPTED.getValue()).build();

        pubSub().publishAfterCommit("sub-5", dto);

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(eq(VerdictPubSub.CHANNEL), body.capture());
        assertThat(body.getValue()).contains("sub-5");
    }

    @Test
    void publishOutput_roundTripsThroughOnMessage() throws Exception {
        SubmissionResponseDto dto = SubmissionResponseDto.builder()
                .status(SubmissionResult.ACCEPTED.getValue()).build();

        pubSub().publish("sub-3", dto);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(eq(VerdictPubSub.CHANNEL), body.capture());

        Message message = mock(Message.class);
        when(message.getBody()).thenReturn(body.getValue().getBytes());
        pubSub().onMessage(message, null);

        verify(sseRegistry).complete(eq("sub-3"), any(SubmissionResponseDto.class));
    }
}
