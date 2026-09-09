package product_service.model;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "Product name is required")
    @Size(min=3, max=100, message="Product name must be between 3 and 100 characters")
    private String name;
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be a positive value")
    private Double price;
    @NotBlank(message = "Category is required")
    @Size(min=2, max=50, message="Category must be between 2 and 50 characters")
    private String category;

    private LocalDateTime createdAt;
    @PositiveOrZero(message = "Stock cannot be negative")
    private Integer stock=0;
    private String status;


    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Product() {
    }

    public Product(Long id, String name, Double price, String category, LocalDateTime createdAt, Integer stock, String status) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.createdAt = createdAt;
        this.stock = stock;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public Integer getStock() {
        return stock;
    }
    public void setStock(Integer stock) {
        this.stock = stock;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    
}