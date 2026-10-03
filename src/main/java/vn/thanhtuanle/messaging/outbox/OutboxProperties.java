package vn.thanhtuanle.messaging.outbox;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "oj.outbox")
@Getter
@Setter
public class OutboxProperties {
    /** Off in the test profile: tests drive {@link OutboxRelay} directly. */
    private boolean relayEnabled = true;
    /** Most rows one relay pass locks and sends. */
    private int batchSize = 100;
    /** Published rows older than this are deleted by the daily clean-up. */
    private int retentionDays = 7;
}
