package vn.thanhtuanle.messaging.outbox;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Sends the stored bytes as they are. It owns a producer instead of declaring a {@code KafkaTemplate}
 * bean, because a second template bean would switch off Boot's auto-configured
 * {@code KafkaTemplate<String, Object>} that the dead-letter recoverer uses.
 */
@Component
public class KafkaOutboxSender implements OutboxSender, DisposableBean {

    private final DefaultKafkaProducerFactory<String, byte[]> producerFactory;
    private final KafkaTemplate<String, byte[]> template;

    public KafkaOutboxSender(KafkaProperties kafkaProperties, ObjectProvider<SslBundles> sslBundles) {
        Map<String, Object> config = kafkaProperties.buildProducerProperties(sslBundles.getIfAvailable());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        // While Kafka is down a relay pass gives up within seconds; the rows wait for the next pass.
        config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 10000);
        this.producerFactory = new DefaultKafkaProducerFactory<>(config);
        this.template = new KafkaTemplate<>(producerFactory);
    }

    @Override
    public void send(OutboxMessage message) throws Exception {
        template.send(message.getTopic(), message.getMessageKey(), message.getPayload()).get(15, TimeUnit.SECONDS);
    }

    @Override
    public void destroy() {
        producerFactory.destroy();
    }
}
