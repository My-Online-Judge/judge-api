package vn.thanhtuanle.messaging.outbox;

/** Puts one outbox message on the wire and returns only once the broker acknowledged it. */
public interface OutboxSender {

    void send(OutboxMessage message) throws Exception;
}
