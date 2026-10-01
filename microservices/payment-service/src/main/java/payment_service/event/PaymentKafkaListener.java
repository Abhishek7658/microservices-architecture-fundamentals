package payment_service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import payment_service.service.PaymentService;

@Component
public class PaymentKafkaListener {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaymentKafkaListener(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(
            topics = "inventory-reserved",
            groupId = "payment-service-group"
    )
    public void handleInventoryReserved(String message) {

        try {
            InventoryReservedEvent event =
                    objectMapper.readValue(
                            message,
                            InventoryReservedEvent.class
                    );

            paymentService.processPayment(event);

        } catch (Exception e) {

            System.out.println(
                    "Failed to process InventoryReserved event: "
                            + e.getMessage()
            );
        }
    }
}