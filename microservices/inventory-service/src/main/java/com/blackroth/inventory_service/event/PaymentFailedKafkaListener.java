package com.blackroth.inventory_service.event;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.blackroth.inventory_service.model.ProcessedPaymentFailure;
import com.blackroth.inventory_service.repository.ProcessedPaymentFailureRepository;
import com.blackroth.inventory_service.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class PaymentFailedKafkaListener {

    private static final String CORRELATION_ID_HEADER =
            "X-Correlation-ID";

    private final InventoryService inventoryService;
    private final InventoryEventPublisher inventoryEventPublisher;
    private final ProcessedPaymentFailureRepository
            processedPaymentFailureRepository;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    public PaymentFailedKafkaListener(
            InventoryService inventoryService,
            InventoryEventPublisher inventoryEventPublisher,
            ProcessedPaymentFailureRepository processedPaymentFailureRepository) {

        this.inventoryService = inventoryService;
        this.inventoryEventPublisher = inventoryEventPublisher;
        this.processedPaymentFailureRepository =
                processedPaymentFailureRepository;
    }

    @KafkaListener(
            topics = "payment-failed",
            groupId = "inventory-service-payment-failed-group"
    )
    public void handlePaymentFailed(
            ConsumerRecord<String, String> record) {

        try {

            String correlationId =
                    extractCorrelationId(record);

            PaymentFailedEvent event =
                    objectMapper.readValue(
                            record.value(),
                            PaymentFailedEvent.class
                    );

            System.out.println(
                    "Received PaymentFailed event | Correlation ID: "
                            + correlationId
                            + " | Order ID: "
                            + event.getOrderId()
            );

            if (processedPaymentFailureRepository
                    .findByOrderId(event.getOrderId())
                    .isPresent()) {

                System.out.println(
                        "Duplicate PaymentFailed event ignored for order: "
                                + event.getOrderId()
                );

                return;
            }

            inventoryService.releaseInventory(
                    event.getProductId(),
                    event.getQuantity()
            );

            InventoryReleasedEvent releasedEvent =
                    new InventoryReleasedEvent(
                            event.getOrderId(),
                            event.getProductId(),
                            event.getQuantity()
                    );

            inventoryEventPublisher.publishInventoryReleased(
                    releasedEvent,
                    correlationId
            );

            try {

                processedPaymentFailureRepository.save(
                        new ProcessedPaymentFailure(
                                event.getOrderId()
                        )
                );

            } catch (DataIntegrityViolationException e) {

                System.out.println(
                        "Duplicate PaymentFailed event ignored for order: "
                                + event.getOrderId()
                );

                return;
            }

            System.out.println(
                    "Inventory released after payment failure for order: "
                            + event.getOrderId()
                            + " | Correlation ID: "
                            + correlationId
            );

        } catch (Exception e) {

            System.out.println(
                    "Failed to process PaymentFailed event: "
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