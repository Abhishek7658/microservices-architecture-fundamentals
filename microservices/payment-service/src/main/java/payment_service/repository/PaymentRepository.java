package payment_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import payment_service.model.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}