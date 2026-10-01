package payment_service.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {

    private static final String PAYMENT_COMPLETED_TOPIC = "payment-completed";
    private static final String PAYMENT_FAILED_TOPIC = "payment-failed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        kafkaTemplate.send(
                PAYMENT_COMPLETED_TOPIC,
                event.getOrderId().toString(),
                event
        );
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        kafkaTemplate.send(
                PAYMENT_FAILED_TOPIC,
                event.getOrderId().toString(),
                event
        );
    }
}