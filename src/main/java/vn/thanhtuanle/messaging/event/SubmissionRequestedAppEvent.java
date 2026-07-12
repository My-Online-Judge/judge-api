package vn.thanhtuanle.messaging.event;

/**
 * Internal (in-JVM) application event fired synchronously inside
 * {@code SubmissionService.submit()}'s transaction. A
 * {@code TransactionalEventListener} bound to {@code AFTER_COMMIT} republishes
 * the wrapped payload to Kafka only once the submission row is durably
 * committed, avoiding a race where the judge worker's verdict arrives before
 * the submission is visible to other transactions.
 */
public record SubmissionRequestedAppEvent(SubmissionRequestedEvent payload) {
}
