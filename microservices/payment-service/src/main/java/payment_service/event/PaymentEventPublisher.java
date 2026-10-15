
package payment_service.event;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(PaymentEventPublisher.class);

    private static final String PAYMENT_COMPLETED_TOPIC =
            "payment-completed";

    private static final String PAYMENT_FAILED_TOPIC =
            "payment-failed";

    private static final String CORRELATION_ID_HEADER =
            "X-Correlation-ID";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPaymentCompleted(
            PaymentCompletedEvent event,
            String correlationId) {

        ProducerRecord<String, Object> record =
                createRecord(
                        PAYMENT_COMPLETED_TOPIC,
                        event.getOrderId().toString(),
                        event,
                        correlationId
                );

        kafkaTemplate.send(record);

        log.info(
                "Published PaymentCompleted event | correlationId={} | orderId={}",
                correlationId,
                event.getOrderId()
        );
    }

    public void publishPaymentFailed(
            PaymentFailedEvent event,
            String correlationId) {

        ProducerRecord<String, Object> record =
                createRecord(
                        PAYMENT_FAILED_TOPIC,
                        event.getOrderId().toString(),
                        event,
                        correlationId
                );

        kafkaTemplate.send(record);

        log.info(
                "Published PaymentFailed event | correlationId={} | orderId={}",
                correlationId,
                event.getOrderId()
        );
    }

    private ProducerRecord<String, Object> createRecord(
            String topic,
            String key,
            Object event,
            String correlationId) {

        ProducerRecord<String, Object> record =
                new ProducerRecord<>(topic, key, event);

        if (correlationId != null && !correlationId.isBlank()) {
            record.headers().add(
                    CORRELATION_ID_HEADER,
                    correlationId.getBytes(StandardCharsets.UTF_8)
            );
        } else {
            log.warn(
                    "Publishing Kafka event without correlation ID | topic={} | key={}",
                    topic,
                    key
            );
        }

        return record;
    }
}