package com.blackroth.inventory_service.event;

import com.blackroth.inventory_service.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryKafkaListener {

    private final InventoryService inventoryService;
    private final InventoryEventPublisher inventoryEventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InventoryKafkaListener(
            InventoryService inventoryService,
            InventoryEventPublisher inventoryEventPublisher) {

        this.inventoryService = inventoryService;
        this.inventoryEventPublisher = inventoryEventPublisher;
    }

    @KafkaListener(
            topics = "order-created",
            groupId = "inventory-service-group"
    )
    public void handleOrderCreated(String message) {

        try {
            OrderCreatedEvent event =
                    objectMapper.readValue(
                            message,
                            OrderCreatedEvent.class
                    );

            boolean reserved = inventoryService.reserveInventory(
                    event.getProductId(),
                    event.getQuantity()
            );

            if (reserved) {

                InventoryReservedEvent reservedEvent =
                        new InventoryReservedEvent(
                                event.getOrderId(),
                                event.getProductId(),
                                event.getQuantity(),
                                event.getAmount()
                        );

                inventoryEventPublisher.publishInventoryReserved(
                        reservedEvent
                );

            } else {

                System.out.println(
                        "Inventory unavailable for order: "
                                + event.getOrderId()
                );
            }

        } catch (Exception e) {

            System.out.println(
                    "Failed to process OrderCreated event: "
                            + e.getMessage()
            );
        }
    }
}