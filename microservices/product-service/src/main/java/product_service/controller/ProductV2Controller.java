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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import product_service.dto.ProductV2Request;
import product_service.dto.ProductV2Response;
import product_service.service.ProductService;

@RestController
@RequestMapping("/api/v2/products")
@Tag(name = "Products", description = "Product management endpoints (v2) with stock and status tracking")
public class ProductV2Controller {

    private final ProductService productService;

    public ProductV2Controller(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "Get all products", description = "Returns all products with computed stock status (AVAILABLE / OUT_OF_STOCK)")
    @ApiResponse(responseCode = "200", description = "List of products retrieved successfully")
    @GetMapping
    public ResponseEntity<Map<String,Object>> getProductsV2(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id,asc") String sort,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) Double minPrice,
        @RequestParam(required = false) Double maxPrice
    )
        
         {
            return ResponseEntity.ok(productService.getProductsV2(page, size, sort, category, minPrice, maxPrice));
        }
    @Operation(summary = "Get a product by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductV2Response> getProductV2ById(
            @Parameter(description = "ID of the product to retrieve", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductV2ById(id));
    }

    @Operation(summary = "Create a new product", description = "Stock defaults to 0 if not provided; status is computed automatically")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
    @PostMapping
    public ResponseEntity<ProductV2Response> createProductV2(@Valid @RequestBody ProductV2Request request) {
        ProductV2Response created = productService.createProductV2(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Fully update an existing product", description = "Replaces all fields; status is recalculated from the new stock value")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ProductV2Response> updateProductV2(
            @Parameter(description = "ID of the product to update", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody ProductV2Request request) {
        return ResponseEntity.ok(productService.updateProductV2(id, request));
    }

    @Operation(summary = "Partially update a product", description = "Only the fields provided in the request body are updated")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product patched successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<ProductV2Response> patchProductV2(
            @Parameter(description = "ID of the product to patch", example = "1")
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(productService.patchProductV2(id, updates));
    }

    @Operation(summary = "Delete a product")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProductV2(
            @Parameter(description = "ID of the product to delete", example = "1")
            @PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}