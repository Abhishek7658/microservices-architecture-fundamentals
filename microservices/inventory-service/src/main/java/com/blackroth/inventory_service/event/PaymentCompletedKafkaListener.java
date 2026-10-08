package com.blackroth.inventory_service.event;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.blackroth.inventory_service.model.ProcessedPaymentCompletion;
import com.blackroth.inventory_service.repository.ProcessedPaymentCompletionRepository;
import com.blackroth.inventory_service.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class PaymentCompletedKafkaListener {

    private static final String CORRELATION_ID_HEADER =
            "X-Correlation-ID";

    private final InventoryService inventoryService;

    private final ProcessedPaymentCompletionRepository
            processedPaymentCompletionRepository;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    public PaymentCompletedKafkaListener(
            InventoryService inventoryService,
            ProcessedPaymentCompletionRepository
                    processedPaymentCompletionRepository) {

        this.inventoryService = inventoryService;

        this.processedPaymentCompletionRepository =
                processedPaymentCompletionRepository;
    }

    @Transactional
    @KafkaListener(
            topics = "payment-completed",
            groupId = "inventory-service-payment-completed-group"
    )
    public void handlePaymentCompleted(
            ConsumerRecord<String, String> record) {

        String correlationId =
                extractCorrelationId(record);

        try {

            PaymentCompletedEvent event =
                    objectMapper.readValue(
                            record.value(),
                            PaymentCompletedEvent.class
                    );

            Long orderId = event.getOrderId();

            System.out.println(
                    "Received PaymentCompleted event"
                            + " | Correlation ID: "
                            + correlationId
                            + " | Order ID: "
                            + orderId
            );

            /*
             * Idempotency check.
             *
             * If this PaymentCompleted event for the order
             * has already been processed, do nothing.
             */
            if (processedPaymentCompletionRepository
                    .findByOrderId(orderId)
                    .isPresent()) {

                System.out.println(
                        "Duplicate PaymentCompleted event ignored"
                                + " | Correlation ID: "
                                + correlationId
                                + " | Order ID: "
                                + orderId
                );

                return;
            }

            /*
             * Complete the inventory sale:
             *
             * reservedQuantity -= quantity
             * soldQuantity     += quantity
             */
            inventoryService.completeSale(
                    event.getProductId(),
                    event.getQuantity()
            );

            /*
             * Record that this PaymentCompleted event
             * has been successfully processed.
             */
            ProcessedPaymentCompletion processedEvent =
                    new ProcessedPaymentCompletion(orderId);

            processedPaymentCompletionRepository.save(
                    processedEvent
            );

            System.out.println(
                    "Inventory sale completed"
                            + " | Correlation ID: "
                            + correlationId
                            + " | Order ID: "
                            + orderId
                            + " | Product ID: "
                            + event.getProductId()
                            + " | Quantity: "
                            + event.getQuantity()
            );

        } catch (Exception e) {

            System.out.println(
                    "Failed to process PaymentCompleted event"
                            + " | Correlation ID: "
                            + correlationId
                            + " | Error: "
                            + e.getMessage()
            );

            throw new RuntimeException(
                    "Failed to process PaymentCompleted event",
                    e
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