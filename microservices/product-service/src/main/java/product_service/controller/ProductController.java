package product_service.controller;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import product_service.model.Product;
import product_service.service.ProductService;

@RestController
@RequestMapping({
        "/products",
        "/api/v1/products"
})
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }


    // CREATE PRODUCT
    @PostMapping
    public ResponseEntity<Product> createProduct(
            @Valid @RequestBody Product product
    ) {

        Product savedProduct =
                productService.createProduct(product);

        return new ResponseEntity<>(
                savedProduct,
                HttpStatus.CREATED
        );
    }


    // PAGINATION + SORTING + FILTERING
    @GetMapping
    public ResponseEntity<?> getProducts(

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "id,asc")
            String sort,

            @RequestParam(required = false)
            String category,

            @RequestParam(required = false)
            Double minPrice,

            @RequestParam(required = false)
            Double maxPrice
    ) {

        return ResponseEntity.ok(
                productService.getProducts(
                        page,
                        size,
                        sort,
                        category,
                        minPrice,
                        maxPrice
                )
        );
    }
   

    // GET PRODUCT BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                productService.getProductById(id)
        );
    }


    // UPDATE PRODUCT
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody Product product
    ) {

        return ResponseEntity.ok(
                productService.updateProduct(id, product)
        );
    }


    // PATCH PRODUCT
    @PatchMapping("/{id}")
    public ResponseEntity<Product> patchProduct(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates
    ) {

        return ResponseEntity.ok(
                productService.patchProduct(id, updates)
        );
    }


    // DELETE PRODUCT
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id
    ) {

        productService.deleteProduct(id);

        return ResponseEntity.noContent().build();
    }
    // PUBLIC PRODUCT LISTING (no auth required)
@GetMapping("/public")
public ResponseEntity<?> getPublicProducts() {

    return ResponseEntity.ok(
            productService.getProducts(
                    0,
                    10,
                    "id,asc",
                    null,
                    null,
                    null
            )
    );
}
}