package payment_service.event;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {

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

        System.out.println(
                "Published PaymentCompleted event | Correlation ID: "
                        + correlationId
                        + " | Order ID: "
                        + event.getOrderId()
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

        System.out.println(
                "Published PaymentFailed event | Correlation ID: "
                        + correlationId
                        + " | Order ID: "
                        + event.getOrderId()
        );
    }

    private ProducerRecord<String, Object> createRecord(
            String topic,
            String key,
            Object event,
            String correlationId) {

        ProducerRecord<String, Object> record =
                new ProducerRecord<>(
                        topic,
                        key,
                        event
                );

        if (correlationId != null
                && !correlationId.isBlank()) {

            record.headers().add(
                    CORRELATION_ID_HEADER,
                    correlationId.getBytes(
                            StandardCharsets.UTF_8
                    )
            );
        }

        return record;
    }
}