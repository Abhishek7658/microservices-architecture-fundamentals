package order_service.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateOrderRequest {

    @NotBlank
    private String status;

    public UpdateOrderRequest() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}