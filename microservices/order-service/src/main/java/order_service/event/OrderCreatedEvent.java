package order_service.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderCreatedEvent {

    private Long orderId;
    private Long userId;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal amount;
    private String eventType;
    private LocalDateTime createdAt;

    public OrderCreatedEvent() {
    }

    public OrderCreatedEvent(
            Long orderId,
            Long userId,
            Long productId,
            String productName,
            Integer quantity,
            BigDecimal amount,
            String eventType,
            LocalDateTime createdAt) {

        this.orderId = orderId;
        this.userId = userId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.amount = amount;
        this.eventType = eventType;
        this.createdAt = createdAt;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
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

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}