package product_service.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class ProductV2Request {

    @Schema(description = "Name of the product", example = "Laptop")
    
    @NotBlank(message = "Product name is required")
    @Size(min=3, max=100, message="Product name must be between 3 and 100 characters")
    private String name;

    @Schema(description = "Price of the product", example = "999.99")
    @NotNull(message = "Product price is required")
    @Positive(message = "Product price must be a positive value")
    private Double price;

    @Schema(description = "Category of the product", example = "Electronics")
    private String category;

    @Schema(description = "Available stock quantity", example = "10")
    @PositiveOrZero(message = "Stock cannot be negative")
    private Integer stock;

    public ProductV2Request() {
    }

    public ProductV2Request(String name, Double price, String category, Integer stock) {
        this.name = name;
        this.price = price;
        this.category = category;
        this.stock = stock;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }
}