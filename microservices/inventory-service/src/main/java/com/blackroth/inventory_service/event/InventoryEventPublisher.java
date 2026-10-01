package com.blackroth.inventory_service.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventPublisher {

    private static final String INVENTORY_RESERVED_TOPIC =
            "inventory-reserved";

    private static final String INVENTORY_RELEASED_TOPIC =
            "inventory-released";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public InventoryEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate) {

        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishInventoryReserved(
            InventoryReservedEvent event) {

        kafkaTemplate.send(
                INVENTORY_RESERVED_TOPIC,
                event.getOrderId().toString(),
                event
        );
    }

    public void publishInventoryReleased(
            InventoryReleasedEvent event) {

        kafkaTemplate.send(
                INVENTORY_RELEASED_TOPIC,
                event.getOrderId().toString(),
                event
        );
    }
}