
package com.blackroth.inventory_service.event;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.blackroth.inventory_service.service.InventoryService;
import tools.jackson.databind.ObjectMapper;

@Component
public class InventoryKafkaListener {

    private static final Logger log =
            LoggerFactory.getLogger(InventoryKafkaListener.class);

    private static final String CORRELATION_ID_HEADER =
            "X-Correlation-ID";

    private final InventoryService inventoryService;
    private final InventoryEventPublisher inventoryEventPublisher;
    private final ObjectMapper objectMapper;

    public InventoryKafkaListener(
            InventoryService inventoryService,
            InventoryEventPublisher inventoryEventPublisher,
            ObjectMapper objectMapper) {
        this.inventoryService = inventoryService;
        this.inventoryEventPublisher = inventoryEventPublisher;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "order-created",
            groupId = "inventory-service-group"
    )
    public void handleOrderCreated(
            ConsumerRecord<String, String> record) {

        String correlationId = extractCorrelationId(record);

        if (correlationId != null && !correlationId.isBlank()) {
            MDC.put("correlationId", correlationId);
        } else {
            MDC.remove("correlationId");
        }

        try {
            OrderCreatedEvent event = objectMapper.readValue(
                    record.value(),
                    OrderCreatedEvent.class
            );

            log.info(
                    "Received OrderCreated event | correlationId={} | orderId={}",
                    correlationId,
                    event.getOrderId()
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
                        reservedEvent,
                        correlationId
                );

                log.info(
                        "Inventory reserved | correlationId={} | orderId={}",
                        correlationId,
                        event.getOrderId()
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

                log.warn(
                        "Inventory unavailable | correlationId={} | orderId={}",
                        correlationId,
                        event.getOrderId()
                );
            }
        } catch (Exception e) {
            log.error(
                    "Failed to process OrderCreated event | correlationId={}",
                    correlationId,
                    e
            );
            // Preserve the existing behavior: log without rethrowing.
        } finally {
            MDC.remove("correlationId");
        }
    }

    private String extractCorrelationId(
            ConsumerRecord<String, String> record) {

        Header header =
                record.headers().lastHeader(CORRELATION_ID_HEADER);

        if (header == null || header.value() == null) {
            return null;
        }

        return new String(
                header.value(),
                StandardCharsets.UTF_8
        );
    }
}
