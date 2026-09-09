package product_service.dto;

import jakarta.validation.constraints.Positive;

public class ProductUpdateRequest {

    private String name;
    @Positive(message = "Product price must be a positive value")
    private Double price;

    public ProductUpdateRequest() {
    }

    public ProductUpdateRequest(String name, Double price) {
        this.name = name;
       
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }
}