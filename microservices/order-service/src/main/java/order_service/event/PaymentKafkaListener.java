package order_service.event;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import order_service.model.Order;
import order_service.repository.OrderRepository;

@Component
public class PaymentKafkaListener {

    private static final String CORRELATION_ID_HEADER =
            "X-Correlation-ID";

    private final OrderRepository orderRepository;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    public PaymentKafkaListener(
            OrderRepository orderRepository) {

        this.orderRepository = orderRepository;
    }

    @KafkaListener(
            topics = "payment-completed",
            groupId = "order-service-payment-group"
    )
    public void handlePaymentCompleted(
            ConsumerRecord<String, String> record) {

        try {

            String correlationId =
                    extractCorrelationId(record);

            PaymentCompletedEvent event =
                    objectMapper.readValue(
                            record.value(),
                            PaymentCompletedEvent.class
                    );

            System.out.println(
                    "Received PaymentCompleted event | Correlation ID: "
                            + correlationId
                            + " | Order ID: "
                            + event.getOrderId()
            );

            Order order =
                    orderRepository
                            .findById(event.getOrderId())
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "Order not found: "
                                                    + event.getOrderId()
                                    )
                            );

            // Idempotency: do not process an already confirmed order
            if ("CONFIRMED".equals(order.getStatus())) {
                return;
            }

            order.setStatus("CONFIRMED");
            orderRepository.save(order);

            System.out.println(
                    "Order confirmed after payment: "
                            + event.getOrderId()
                            + " | Correlation ID: "
                            + correlationId
            );

        } catch (Exception e) {

            System.out.println(
                    "Failed to process PaymentCompleted event: "
                            + e.getMessage()
            );
        }
    }

    private String extractCorrelationId(
            ConsumerRecord<String, String> record) {

        Header header =
                record.headers().lastHeader(
                        CORRELATION_ID_HEADER
                );

        if (header == null) {
            return null;
        }

        return new String(
                header.value(),
                StandardCharsets.UTF_8
        );
    }
}