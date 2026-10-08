package payment_service.event;

import java.math.BigDecimal;

public class PaymentCompletedEvent {

    private Long orderId;
    private Long productId;
    private Integer quantity;
    private BigDecimal amount;
    private String transactionReference;

    public PaymentCompletedEvent() {
    }

    public PaymentCompletedEvent(
            Long orderId,
            Long productId,
            Integer quantity,
            BigDecimal amount,
            String transactionReference) {

        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.amount = amount;
        this.transactionReference = transactionReference;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
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

    public void setTransactionReference(
            String transactionReference) {

        this.transactionReference =
                transactionReference;
    }
}