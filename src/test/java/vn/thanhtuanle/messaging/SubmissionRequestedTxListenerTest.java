package vn.thanhtuanle.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.thanhtuanle.messaging.event.SubmissionRequestedAppEvent;
import vn.thanhtuanle.messaging.event.SubmissionRequestedEvent;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SubmissionRequestedTxListenerTest {

    @Mock SubmissionEventPublisher submissionEventPublisher;
    @InjectMocks SubmissionRequestedTxListener listener;

    @Test
    void onAfterCommit_publishesWrappedPayload() {
        SubmissionRequestedEvent event = SubmissionRequestedEvent.builder().submissionId("x").build();

        listener.onAfterCommit(new SubmissionRequestedAppEvent(event));

        verify(submissionEventPublisher).publishRequested(event);
    }
}
