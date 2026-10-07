package com.blackroth.inventory_service.event;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventPublisher {

    private static final String INVENTORY_RESERVED_TOPIC =
            "inventory-reserved";

    private static final String INVENTORY_RELEASED_TOPIC =
            "inventory-released";

    private static final String INVENTORY_RESERVATION_FAILED_TOPIC =
            "inventory-reservation-failed";

    private static final String CORRELATION_ID_HEADER =
            "X-Correlation-ID";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public InventoryEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate) {

        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishInventoryReserved(
            InventoryReservedEvent event,
            String correlationId) {

        ProducerRecord<String, Object> record =
                createRecord(
                        INVENTORY_RESERVED_TOPIC,
                        event.getOrderId().toString(),
                        event,
                        correlationId
                );

        kafkaTemplate.send(record);

        System.out.println(
                "Published InventoryReserved event | Correlation ID: "
                        + correlationId
                        + " | Order ID: "
                        + event.getOrderId()
        );
    }

    public void publishInventoryReleased(
            InventoryReleasedEvent event,
            String correlationId) {

        ProducerRecord<String, Object> record =
                createRecord(
                        INVENTORY_RELEASED_TOPIC,
                        event.getOrderId().toString(),
                        event,
                        correlationId
                );

        kafkaTemplate.send(record);

        System.out.println(
                "Published InventoryReleased event | Correlation ID: "
                        + correlationId
                        + " | Order ID: "
                        + event.getOrderId()
        );
    }

    public void publishInventoryReservationFailed(
            InventoryReservationFailedEvent event,
            String correlationId) {

        ProducerRecord<String, Object> record =
                createRecord(
                        INVENTORY_RESERVATION_FAILED_TOPIC,
                        event.getOrderId().toString(),
                        event,
                        correlationId
                );

        kafkaTemplate.send(record);

        System.out.println(
                "Published InventoryReservationFailed event | Correlation ID: "
                        + correlationId
                        + " | Order ID: "
                        + event.getOrderId()
        );
    }

    private ProducerRecord<String, Object> createRecord(
            String topic,
            String key,
            Object event,
            String correlationId) {

        ProducerRecord<String, Object> record =
                new ProducerRecord<>(
                        topic,
                        key,
                        event
                );

        if (correlationId != null
                && !correlationId.isBlank()) {

            record.headers().add(
                    CORRELATION_ID_HEADER,
                    correlationId.getBytes(
                            StandardCharsets.UTF_8
                    )
            );
        }

        return record;
    }
}