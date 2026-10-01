package payment_service.event;

import java.math.BigDecimal;

public class InventoryReservedEvent {

    private Long orderId;
    private Long productId;
    private Integer quantity;
    private BigDecimal amount;

    public InventoryReservedEvent() {
    }

    public InventoryReservedEvent(
            Long orderId,
            Long productId,
            Integer quantity,
            BigDecimal amount) {

        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.amount = amount;
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
}