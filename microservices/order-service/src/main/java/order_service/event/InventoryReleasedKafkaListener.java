package order_service.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import order_service.model.Order;
import order_service.repository.OrderRepository;

@Component
public class InventoryReleasedKafkaListener {

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InventoryReleasedKafkaListener(
            OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @KafkaListener(
            topics = "inventory-released",
            groupId = "order-service-inventory-released-group"
    )
    public void handleInventoryReleased(String message) {

        try {
            InventoryReleasedEvent event =
                    objectMapper.readValue(
                            message,
                            InventoryReleasedEvent.class
                    );

            Order order = orderRepository.findById(event.getOrderId())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Order not found: " + event.getOrderId()
                            ));

            if ("CANCELLED".equals(order.getStatus())) {
                return;
            }

            order.setStatus("CANCELLED");
            orderRepository.save(order);

            System.out.println(
                    "Order cancelled after inventory release: "
                            + event.getOrderId()
            );

        } catch (Exception e) {
            System.out.println(
                    "Failed to process InventoryReleased event: "
                            + e.getMessage()
            );
        }
    }
}