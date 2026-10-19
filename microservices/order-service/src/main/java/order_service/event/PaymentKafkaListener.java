package order_service.event;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import order_service.model.Order;
import order_service.repository.OrderRepository;

@Component
public class PaymentKafkaListener {

    private static final Logger log =
            LoggerFactory.getLogger(PaymentKafkaListener.class);

    private static final String CORRELATION_ID_HEADER =
            "X-Correlation-ID";

    private final OrderRepository orderRepository;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    public PaymentKafkaListener(
            OrderRepository orderRepository) {

        this.orderRepository = orderRepository;
    }

    @KafkaListener(
            topics = "payment-completed",
            groupId = "order-service-payment-group"
    )
    public void handlePaymentCompleted(
            ConsumerRecord<String, String> record) {

        String correlationId =
                extractCorrelationId(record);

        // Make the ID appear in the [correlationId=...] part of every log line.
        if (correlationId != null && !correlationId.isBlank()) {
            MDC.put("correlationId", correlationId);
        }

        try {

            PaymentCompletedEvent event =
                    objectMapper.readValue(
                            record.value(),
                            PaymentCompletedEvent.class
                    );

            log.info(
                    "Received PaymentCompleted event | orderId={} | productId={} | quantity={}",
                    event.getOrderId(),
                    event.getProductId(),
                    event.getQuantity()
            );

            Order order =
                    orderRepository
                            .findById(event.getOrderId())
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "Order not found: "
                                                    + event.getOrderId()
                                    )
                            );

            /*
             * Idempotency:
             * If the same PaymentCompleted event is delivered again,
             * do not confirm the order a second time.
             */
            if ("CONFIRMED".equals(order.getStatus())) {

                log.info(
                        "PaymentCompleted already processed | Order already CONFIRMED | orderId={}",
                        event.getOrderId()
                );

                return;
            }

            order.setStatus("CONFIRMED");
            order.setUpdatedAt(LocalDateTime.now());

            orderRepository.save(order);

            log.info(
                    "Order confirmed after payment | orderId={}",
                    event.getOrderId()
            );

        } catch (Exception e) {

            log.error(
                    "Failed to process PaymentCompleted event | correlationId={}",
                    correlationId,
                    e
            );

            /*
             * Do not silently swallow Kafka processing failures.
             * Propagating the exception allows Spring Kafka's
             * error-handling mechanism to detect the failure.
             */
            throw new IllegalStateException(
                    "Failed to process PaymentCompleted event",
                    e
            );

        } finally {
            // Listener threads are reused, so always clear the MDC.
            MDC.remove("correlationId");
        }
    }

    private String extractCorrelationId(
            ConsumerRecord<String, String> record) {

        Header header =
                record.headers().lastHeader(
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