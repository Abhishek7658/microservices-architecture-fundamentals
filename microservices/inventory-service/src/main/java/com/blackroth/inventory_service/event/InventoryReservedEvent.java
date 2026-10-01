package com.blackroth.inventory_service.event;

public class InventoryReservedEvent {

    private Long orderId;
    private Long productId;
    private Integer quantity;
    private Double amount;

    public InventoryReservedEvent() {
    }

    public InventoryReservedEvent(
            Long orderId,
            Long productId,
            Integer quantity,
            Double amount) {

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

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}