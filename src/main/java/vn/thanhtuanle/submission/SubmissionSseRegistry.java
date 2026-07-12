package vn.thanhtuanle.submission;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import vn.thanhtuanle.submission.dto.SubmissionResponseDto;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of SSE subscribers keyed by submissionId.
 * NOTE: single-instance only; multi-instance fan-out would need a shared bus (out of scope).
 */
@Component
@Slf4j
public class SubmissionSseRegistry {

    private static final long TIMEOUT_MS = 5 * 60 * 1000L;
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String submissionId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emitters.put(submissionId, emitter);
        emitter.onCompletion(() -> emitters.remove(submissionId));
        emitter.onTimeout(() -> emitters.remove(submissionId));
        return emitter;
    }

    public void complete(String submissionId, SubmissionResponseDto payload) {
        SseEmitter emitter = emitters.remove(submissionId);
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name("verdict").data(payload));
            emitter.complete();
        } catch (IOException e) {
            log.warn("Failed to push SSE verdict for {}", submissionId, e);
            emitter.completeWithError(e);
        }
    }
}
