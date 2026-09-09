package product_service.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import product_service.dto.ProductV2Request;
import product_service.dto.ProductV2Response;
import product_service.model.Product;
import product_service.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    private final List<String> allowedSortFields =
            Arrays.asList("id", "name", "price", "category");

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // CREATE
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    // GET BY ID
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Product not found with id: " + id
                        )
                );
    }

    // UPDATE
    public Product updateProduct(Long id, Product updatedProduct) {

        Product existingProduct = getProductById(id);

        existingProduct.setName(updatedProduct.getName());
        existingProduct.setPrice(updatedProduct.getPrice());
        existingProduct.setCategory(updatedProduct.getCategory());

        return productRepository.save(existingProduct);
    }

    // PATCH
    public Product patchProduct(
            Long id,
            Map<String, Object> updates
    ) {

        Product existingProduct = getProductById(id);

        if (updates.containsKey("name")) {
            existingProduct.setName(
                    (String) updates.get("name")
            );
        }

        if (updates.containsKey("price")) {
            existingProduct.setPrice(
                    Double.valueOf(
                            updates.get("price").toString()
                    )
            );
        }

        if (updates.containsKey("category")) {
            existingProduct.setCategory(
                    (String) updates.get("category")
            );
        }

        return productRepository.save(existingProduct);
    }

    // DELETE
    public void deleteProduct(Long id) {

        Product product = getProductById(id);

        productRepository.delete(product);
    }

    // Helper to convert Product entity -> V2 response shape
    private ProductV2Response toV2Response(Product product) {
        String status = (product.getStock() != null && product.getStock() > 0)
                ? "AVAILABLE" : "OUT_OF_STOCK";
        return new ProductV2Response(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getCategory(),
                product.getStock(),
                status
        );
    }

    // GET single product (v2)
    public ProductV2Response getProductV2ById(Long id) {
        Product product = getProductById(id);
        return toV2Response(product);
    }

    // POST - create (v2)
    public ProductV2Response createProductV2(ProductV2Request request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setCategory(request.getCategory());
        product.setStock(Optional.ofNullable(request.getStock()).orElse(0));

        Product saved = productRepository.save(product);
        return toV2Response(saved);
    }

    // PUT - full update (v2)
    public ProductV2Response updateProductV2(Long id, ProductV2Request request) {
        Product existing = getProductById(id);
        existing.setName(request.getName());
        existing.setPrice(request.getPrice());
        existing.setCategory(request.getCategory());
        existing.setStock(Optional.ofNullable(request.getStock()).orElse(0));

        Product updated = productRepository.save(existing);
        return toV2Response(updated);
    }

    // PATCH - partial update (v2)
    public ProductV2Response patchProductV2(Long id, Map<String, Object> updates) {
        Product existing = getProductById(id);

        if (updates.containsKey("name")) {
            existing.setName((String) updates.get("name"));
        }
        if (updates.containsKey("price")) {
            existing.setPrice(Double.valueOf(updates.get("price").toString()));
        }
        if (updates.containsKey("category")) {
            existing.setCategory((String) updates.get("category"));
        }
        if (updates.containsKey("stock")) {
            existing.setStock(Integer.valueOf(updates.get("stock").toString()));
        }

        Product saved = productRepository.save(existing);
        return toV2Response(saved);
    }

    // Shared helper: builds a validated Page<Product> from pagination/sort/filter params.
    // Used by both v1 getProducts() and v2 getProductsV2() so their behavior never drifts apart.
    private Page<Product> fetchProductPage(
            int page,
            int size,
            String sort,
            String category,
            Double minPrice,
            Double maxPrice
    ) {

        // PAGE VALIDATION
        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative"
            );
        }

        // SIZE VALIDATION
        if (size <= 0) {
            throw new IllegalArgumentException(
                    "Page size must be greater than zero"
            );
        }

        // PRICE RANGE VALIDATION
        if (minPrice != null
                && maxPrice != null
                && minPrice > maxPrice) {

            throw new IllegalArgumentException(
                    "minPrice cannot be greater than maxPrice"
            );
        }

        // SORT VALIDATION
        String[] sortParams = sort.split(",");

        String sortField = sortParams[0];

        if (!allowedSortFields.contains(sortField)) {
            throw new IllegalArgumentException(
                    "Invalid sort field: " + sortField
            );
        }

        Sort.Direction direction = Sort.Direction.ASC;

        if (sortParams.length > 1) {

            if (sortParams[1].equalsIgnoreCase("desc")) {

                direction = Sort.Direction.DESC;

            } else if (!sortParams[1].equalsIgnoreCase("asc")) {

                throw new IllegalArgumentException(
                        "Sort direction must be asc or desc"
                );
            }
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(direction, sortField)
        );

        Page<Product> productPage;

        // CATEGORY + PRICE RANGE
        if (category != null
                && minPrice != null
                && maxPrice != null) {

            productPage =
                    productRepository.findByCategoryAndPriceBetween(
                            category,
                            minPrice,
                            maxPrice,
                            pageable
                    );

        // ONLY CATEGORY
        } else if (category != null) {

            productPage =
                    productRepository.findByCategory(
                            category,
                            pageable
                    );

        // ONLY PRICE RANGE
        } else if (minPrice != null
                && maxPrice != null) {

            productPage =
                    productRepository.findByPriceBetween(
                            minPrice,
                            maxPrice,
                            pageable
                    );

        // NO FILTER
        } else {

            productPage =
                    productRepository.findAll(pageable);
        }

        return productPage;
    }

    // PAGINATION + SORTING + FILTERING (v1)
    public Map<String, Object> getProducts(
            int page,
            int size,
            String sort,
            String category,
            Double minPrice,
            Double maxPrice
    ) {

        Page<Product> productPage =
                fetchProductPage(page, size, sort, category, minPrice, maxPrice);

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "content",
                productPage.getContent()
        );

        response.put(
                "page",
                productPage.getNumber()
        );

        response.put(
                "size",
                productPage.getSize()
        );

        response.put(
                "totalElements",
                productPage.getTotalElements()
        );

        response.put(
                "totalPages",
                productPage.getTotalPages()
        );

        return response;
    }

    // PAGINATION + SORTING + FILTERING (v2)
    public Map<String, Object> getProductsV2(
            int page,
            int size,
            String sort,
            String category,
            Double minPrice,
            Double maxPrice
    ) {

        Page<Product> productPage =
                fetchProductPage(page, size, sort, category, minPrice, maxPrice);

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "content",
                productPage.getContent().stream()
                        .map(this::toV2Response)
                        .collect(Collectors.toList())
        );

        response.put(
                "page",
                productPage.getNumber()
        );

        response.put(
                "size",
                productPage.getSize()
        );

        response.put(
                "totalElements",
                productPage.getTotalElements()
        );

        response.put(
                "totalPages",
                productPage.getTotalPages()
        );

        return response;
    }

}