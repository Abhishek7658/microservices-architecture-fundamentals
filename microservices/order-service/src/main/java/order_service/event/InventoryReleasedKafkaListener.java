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
public class InventoryReleasedKafkaListener {

    private static final String CORRELATION_ID_HEADER =
            "X-Correlation-ID";

    private final OrderRepository orderRepository;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    public InventoryReleasedKafkaListener(
            OrderRepository orderRepository) {

        this.orderRepository = orderRepository;
    }

    @KafkaListener(
            topics = "inventory-released",
            groupId = "order-service-inventory-released-group"
    )
    public void handleInventoryReleased(
            ConsumerRecord<String, String> record) {

        try {

            String correlationId =
                    extractCorrelationId(record);

            InventoryReleasedEvent event =
                    objectMapper.readValue(
                            record.value(),
                            InventoryReleasedEvent.class
                    );

            System.out.println(
                    "Received InventoryReleased event | Correlation ID: "
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

            if ("CANCELLED".equals(order.getStatus())) {
                return;
            }

            order.setStatus("CANCELLED");
            orderRepository.save(order);

            System.out.println(
                    "Order cancelled after inventory release: "
                            + event.getOrderId()
                            + " | Correlation ID: "
                            + correlationId
            );

        } catch (Exception e) {

            System.out.println(
                    "Failed to process InventoryReleased event: "
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