package com.blackroth.inventory_service.event;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.blackroth.inventory_service.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class InventoryKafkaListener {

    private static final String CORRELATION_ID_HEADER =
            "X-Correlation-ID";

    private final InventoryService inventoryService;
    private final InventoryEventPublisher inventoryEventPublisher;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

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
    public void handleOrderCreated(
            ConsumerRecord<String, String> record) {

        try {

            String message = record.value();

            String correlationId =
                    extractCorrelationId(record);

            System.out.println(
                    "Received OrderCreated event | Correlation ID: "
                            + correlationId
                            + " | Order ID from message"
            );

            OrderCreatedEvent event =
                    objectMapper.readValue(
                            message,
                            OrderCreatedEvent.class
                    );

            boolean reserved =
                    inventoryService.reserveInventory(
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

                inventoryEventPublisher
                        .publishInventoryReserved(
                                reservedEvent,
                                correlationId
                        );

                System.out.println(
                        "Inventory reserved | Correlation ID: "
                                + correlationId
                                + " | Order ID: "
                                + event.getOrderId()
                );

            } else {

                InventoryReservationFailedEvent failedEvent =
                        new InventoryReservationFailedEvent(
                                event.getOrderId(),
                                event.getProductId(),
                                event.getQuantity(),
                                "Insufficient inventory"
                        );

                inventoryEventPublisher
                        .publishInventoryReservationFailed(
                                failedEvent,
                                correlationId
                        );

                System.out.println(
                        "Inventory unavailable | Correlation ID: "
                                + correlationId
                                + " | Order ID: "
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