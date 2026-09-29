package notification_service.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import notification_service.event.OrderCreatedEvent;

@Component
public class OrderCreatedConsumer {

    @KafkaListener(
            topics = "order-created",
            groupId = "notification-service"
    )
    public void consume(OrderCreatedEvent event) {

        System.out.println("Order created: " + event.getOrderId());

        System.out.println("User notification required");
    }
}