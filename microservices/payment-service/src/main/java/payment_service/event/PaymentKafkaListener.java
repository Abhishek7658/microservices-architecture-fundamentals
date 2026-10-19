
package payment_service.event;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import payment_service.service.PaymentService;
import tools.jackson.databind.ObjectMapper;

@Component
public class PaymentKafkaListener {

    private static final Logger log =
            LoggerFactory.getLogger(PaymentKafkaListener.class);

    private static final String CORRELATION_ID_HEADER =
            "X-Correlation-ID";

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    public PaymentKafkaListener(
            PaymentService paymentService,
            ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "inventory-reserved",
            groupId = "payment-service-group"
    )
    public void handleInventoryReserved(
            ConsumerRecord<String, String> record) {

        String correlationId = extractCorrelationId(record);

        if (correlationId != null && !correlationId.isBlank()) {
            MDC.put("correlationId", correlationId);
        } else {
            MDC.remove("correlationId");
        }

        try {
            InventoryReservedEvent event = objectMapper.readValue(
                    record.value(),
                    InventoryReservedEvent.class
            );

            log.info(
                    "Received InventoryReserved event | correlationId={} | orderId={}",
                    correlationId,
                    event.getOrderId()
            );

            paymentService.processPayment(event, correlationId);

        } catch (Exception e) {
            log.error(
                    "Failed to process InventoryReserved event | correlationId={}",
                    correlationId,
                    e
            );
            // Preserve existing behavior: log without rethrowing.
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
