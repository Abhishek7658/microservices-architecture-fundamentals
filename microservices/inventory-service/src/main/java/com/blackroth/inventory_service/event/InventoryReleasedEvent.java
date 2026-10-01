package com.blackroth.inventory_service.event;

public class InventoryReleasedEvent {

    private Long orderId;
    private Long productId;
    private Integer quantity;

    public InventoryReleasedEvent() {
    }

    public InventoryReleasedEvent(
            Long orderId,
            Long productId,
            Integer quantity) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
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
}