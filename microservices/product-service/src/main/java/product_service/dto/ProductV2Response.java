package product_service.dto;

public class ProductV2Response {

    private final Long id;
    private final String name;
    private final Double price;
    private final String category;
    private final Integer stock;
    private final String status;

    public ProductV2Response(
            Long id,
            String name,
            Double price,
            String category,
            Integer stock,
            String status
    ) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.stock = stock;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Double getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }

    public Integer getStock() {
        return stock;
    }

    public String getStatus() {
        return status;
    }
}