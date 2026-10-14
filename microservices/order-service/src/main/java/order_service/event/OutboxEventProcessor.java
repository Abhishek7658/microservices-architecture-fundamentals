
package order_service.event;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OutboxEventProcessor {

    private static final Logger log =
            LoggerFactory.getLogger(OutboxEventProcessor.class);

    private static final String ORDER_CREATED_TOPIC = "order-created";

    private final OutboxEventStatusService statusService;
    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxEventProcessor(
            OutboxEventRepository repository,
            KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate,
            ObjectMapper objectMapper,
            OutboxEventStatusService statusService) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.statusService = statusService;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {
        List<OutboxEvent> events =
                repository.findTop100ByStatusOrderByCreatedAtAsc("PENDING");

        for (OutboxEvent event : events) {
            try {
                OrderCreatedEvent payload = objectMapper.readValue(
                        event.getPayload(),
                        OrderCreatedEvent.class);

                var message = MessageBuilder.withPayload(payload)
                        .setHeader(KafkaHeaders.TOPIC, ORDER_CREATED_TOPIC)
                        .setHeader(KafkaHeaders.KEY, event.getAggregateId())
                        .build();

                kafkaTemplate.send(message).get(10, TimeUnit.SECONDS);

                statusService.markPublished(event.getId());

                log.info(
                        "Outbox event published successfully: id={}",
                        event.getId());

            } catch (Exception ex) {
                log.error(
                        "Outbox event publication failed; event remains eligible for retry: id={}",
                        event.getId(),
                        ex);
            }
        }
    }
}
