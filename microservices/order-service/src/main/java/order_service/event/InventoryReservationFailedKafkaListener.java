package order_service.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import order_service.model.Order;
import order_service.repository.OrderRepository;

@Component
public class InventoryReservationFailedKafkaListener {

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InventoryReservationFailedKafkaListener(
            OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @KafkaListener(
            topics = "inventory-reservation-failed",
            groupId = "order-service-inventory-failed-group"
    )
    public void handleInventoryReservationFailed(String message) {

        try {
            InventoryReservationFailedEvent event =
                    objectMapper.readValue(
                            message,
                            InventoryReservationFailedEvent.class
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
                    "Order cancelled due to inventory reservation failure: "
                            + event.getOrderId()
            );

        } catch (Exception e) {
            System.out.println(
                    "Failed to process InventoryReservationFailed event: "
                            + e.getMessage()
            );
        }
    }
}