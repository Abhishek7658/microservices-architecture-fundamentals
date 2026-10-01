package order_service.event;

import java.math.BigDecimal;

public class PaymentCompletedEvent {

    private Long orderId;
    private BigDecimal amount;
    private String transactionReference;

    public PaymentCompletedEvent() {
    }

    public PaymentCompletedEvent(
            Long orderId,
            BigDecimal amount,
            String transactionReference) {

        this.orderId = orderId;
        this.amount = amount;
        this.transactionReference = transactionReference;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }
}