package order_service.event;

import java.nio.charset.StandardCharsets;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Component
public class OrderEventPublisher {

    private static final String ORDER_CREATED_TOPIC =
            "order-created";

    private static final String CORRELATION_ID_HEADER =
            "X-Correlation-ID";

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    public OrderEventPublisher(
            KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderCreated(OrderCreatedEvent event) {

        String correlationId = getCorrelationId();

        MessageBuilder<OrderCreatedEvent> builder =
                MessageBuilder.withPayload(event)
                        .setHeader(
                                KafkaHeaders.TOPIC,
                                ORDER_CREATED_TOPIC
                        )
                        .setHeader(
                                KafkaHeaders.KEY,
                                event.getOrderId().toString()
                        );

        if (correlationId != null) {
            builder.setHeader(
                    CORRELATION_ID_HEADER,
                    correlationId.getBytes(StandardCharsets.UTF_8)
            );
        }

        Message<OrderCreatedEvent> message =
                builder.build();

        kafkaTemplate.send(message);
    }

    private String getCorrelationId() {

        RequestAttributes attributes =
                RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return null;
        }

        Object correlationId =
                attributes.getAttribute(
                        CORRELATION_ID_HEADER,
                        RequestAttributes.SCOPE_REQUEST
                );

        return correlationId != null
                ? correlationId.toString()
                : null;
    }
}