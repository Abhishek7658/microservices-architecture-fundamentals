package payment_service.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import payment_service.event.InventoryReservedEvent;
import payment_service.event.PaymentCompletedEvent;
import payment_service.event.PaymentEventPublisher;
import payment_service.event.PaymentFailedEvent;
import payment_service.model.Payment;
import payment_service.repository.PaymentRepository;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher paymentEventPublisher;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentEventPublisher paymentEventPublisher) {

        this.paymentRepository = paymentRepository;
        this.paymentEventPublisher = paymentEventPublisher;
    }

    // Saga payment processing
    @Transactional
    public void processPayment(InventoryReservedEvent event) {

        // Prevent duplicate payment processing
        if (paymentRepository.findByOrderId(event.getOrderId()).isPresent()) {
            return;
        }

        BigDecimal amount = event.getAmount();

        // Payment failure condition
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {

            PaymentFailedEvent failedEvent =
                    new PaymentFailedEvent(
                            event.getOrderId(),
                            event.getProductId(),
                            event.getQuantity(),
                            "Payment failed: invalid payment amount"
                    );

            paymentEventPublisher.publishPaymentFailed(failedEvent);

            return;
        }

        // Create payment
        Payment payment = new Payment();

        payment.setOrderId(event.getOrderId());
        payment.setAmount(amount.doubleValue());
        payment.setStatus("COMPLETED");
        payment.setTransactionReference(
                "TXN-" + UUID.randomUUID()
        );

        Payment savedPayment = paymentRepository.save(payment);

        // Publish PaymentCompleted event
        PaymentCompletedEvent completedEvent =
                new PaymentCompletedEvent(
                        savedPayment.getOrderId(),
                        BigDecimal.valueOf(savedPayment.getAmount()),
                        savedPayment.getTransactionReference()
                );

        paymentEventPublisher.publishPaymentCompleted(completedEvent);
    }

    // Create payment
    public Payment createPayment(Payment payment) {
        return paymentRepository.save(payment);
    }

    // Get all payments
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    // Get payment by ID
    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    // Update payment
    public Payment updatePayment(Long id, Payment updatedPayment) {

        Payment existingPayment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        existingPayment.setOrderId(updatedPayment.getOrderId());
        existingPayment.setAmount(updatedPayment.getAmount());
        existingPayment.setStatus(updatedPayment.getStatus());
        existingPayment.setTransactionReference(
                updatedPayment.getTransactionReference()
        );

        return paymentRepository.save(existingPayment);
    }

    // Delete payment
    public void deletePayment(Long id) {
        Payment payment = getPaymentById(id);
        paymentRepository.delete(payment);
    }
}