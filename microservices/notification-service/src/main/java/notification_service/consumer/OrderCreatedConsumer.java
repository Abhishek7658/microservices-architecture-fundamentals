
package notification_service.consumer;
import java.nio.charset.StandardCharsets;
import tools.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


import notification_service.event.OrderCreatedEvent;

@Component
public class OrderCreatedConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(OrderCreatedConsumer.class);

    private static final String CORRELATION_ID_HEADER =
            "X-Correlation-ID";

    private final ObjectMapper objectMapper;

    public OrderCreatedConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "order-created",
            groupId = "notification-service"
    )
    public void consume(ConsumerRecord<String, String> record) {

        String correlationId = extractCorrelationId(record);

        try {
            OrderCreatedEvent event = objectMapper.readValue(
                    record.value(),
                    OrderCreatedEvent.class
            );

            log.info(
                    "Order created event received | correlationId={} | orderId={}",
                    correlationId,
                    event.getOrderId()
            );

            log.info(
                    "User notification required | correlationId={} | orderId={}",
                    correlationId,
                    event.getOrderId()
            );

        } catch (Exception e) {
            log.error(
                    "Failed to process OrderCreated event | correlationId={}",
                    correlationId,
                    e
            );
            throw new IllegalStateException(
                    "Failed to process OrderCreated event",
                    e
            );
        }
    }

    private String extractCorrelationId(
            ConsumerRecord<String, String> record) {

        Header header = record.headers().lastHeader(
                CORRELATION_ID_HEADER
        );

        if (header == null || header.value() == null) {
            return null;
        }

        return new String(
                header.value(),
                StandardCharsets.UTF_8
        );
    }
}
