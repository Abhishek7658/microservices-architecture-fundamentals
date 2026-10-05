package order_service.event;

public class InventoryReservationFailedEvent {

    private Long orderId;
    private Long productId;
    private Integer quantity;
    private String reason;

    public InventoryReservationFailedEvent() {
    }

    public InventoryReservationFailedEvent(
            Long orderId,
            Long productId,
            Integer quantity,
            String reason) {

        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.reason = reason;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}