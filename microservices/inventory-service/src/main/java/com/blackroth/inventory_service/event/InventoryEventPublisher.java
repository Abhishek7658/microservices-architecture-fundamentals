
package com.blackroth.inventory_service.event;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(InventoryEventPublisher.class);

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

        ProducerRecord<String, Object> record = createRecord(
                INVENTORY_RESERVED_TOPIC,
                event.getOrderId().toString(),
                event,
                correlationId
        );

        kafkaTemplate.send(record);

        log.info(
                "Published InventoryReserved event | correlationId={} | orderId={}",
                correlationId,
                event.getOrderId()
        );
    }

    public void publishInventoryReleased(
            InventoryReleasedEvent event,
            String correlationId) {

        ProducerRecord<String, Object> record = createRecord(
                INVENTORY_RELEASED_TOPIC,
                event.getOrderId().toString(),
                event,
                correlationId
        );

        kafkaTemplate.send(record);

        log.info(
                "Published InventoryReleased event | correlationId={} | orderId={}",
                correlationId,
                event.getOrderId()
        );
    }

    public void publishInventoryReservationFailed(
            InventoryReservationFailedEvent event,
            String correlationId) {

        ProducerRecord<String, Object> record = createRecord(
                INVENTORY_RESERVATION_FAILED_TOPIC,
                event.getOrderId().toString(),
                event,
                correlationId
        );

        kafkaTemplate.send(record);

        log.info(
                "Published InventoryReservationFailed event | correlationId={} | orderId={}",
                correlationId,
                event.getOrderId()
        );
    }

    private ProducerRecord<String, Object> createRecord(
            String topic,
            String key,
            Object event,
            String correlationId) {

        ProducerRecord<String, Object> record =
                new ProducerRecord<>(topic, key, event);

        if (correlationId != null && !correlationId.isBlank()) {
            record.headers().add(
                    CORRELATION_ID_HEADER,
                    correlationId.getBytes(StandardCharsets.UTF_8)
            );
        } else {
            log.warn(
                    "Publishing event without correlation ID | topic={} | key={}",
                    topic,
                    key
            );
        }

        return record;
    }
}
