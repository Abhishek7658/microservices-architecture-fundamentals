package payment_service.event;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import payment_service.service.PaymentService;

@Component
public class PaymentKafkaListener {

    private static final String CORRELATION_ID_HEADER =
            "X-Correlation-ID";

    private final PaymentService paymentService;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    public PaymentKafkaListener(
            PaymentService paymentService) {

        this.paymentService = paymentService;
    }

    @KafkaListener(
            topics = "inventory-reserved",
            groupId = "payment-service-group"
    )
    public void handleInventoryReserved(
            ConsumerRecord<String, String> record) {

        try {

            String correlationId =
                    extractCorrelationId(record);

            System.out.println(
                    "Received InventoryReserved event | Correlation ID: "
                            + correlationId
            );

            InventoryReservedEvent event =
                    objectMapper.readValue(
                            record.value(),
                            InventoryReservedEvent.class
                    );

            paymentService.processPayment(
                    event,
                    correlationId
            );

        } catch (Exception e) {

            System.out.println(
                    "Failed to process InventoryReserved event: "
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