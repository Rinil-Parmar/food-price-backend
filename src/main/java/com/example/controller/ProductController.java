package com.example.controller;

import com.example.model.ApiResponse;
import com.example.model.Product;
import com.example.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // GET /api/products?page=0&size=20
    @GetMapping
    public ApiResponse<List<Product>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<Product> products = productService.getAllProducts(page, size);
        return new ApiResponse<>("success", "Products retrieved successfully", products);
    }

    // GET /api/products/category/{category}?page=0&size=20
    @GetMapping("/category/{category}")
    public ApiResponse<List<Product>> getProductsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<Product> products = productService.getProductsByCategory(category, page, size);
        if (products.isEmpty()) {
            return new ApiResponse<>("error", "No products found for category: " + category, null);
        }
        return new ApiResponse<>("success", "Products retrieved successfully", products);
    }

    // GET /api/products/store/{storeName}?page=0&size=20
    @GetMapping("/store/{storeName}")
    public ApiResponse<List<Product>> getProductsByStore(
            @PathVariable String storeName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<Product> products = productService.getProductsByStore(storeName, page, size);
        if (products.isEmpty()) {
            return new ApiResponse<>("error", "No products found for store: " + storeName, null);
        }
        return new ApiResponse<>("success", "Products retrieved successfully", products);
    }

    // GET /api/products/{id}
    @GetMapping("/{id}")
    public ApiResponse<Product> getProductById(@PathVariable String id) {
        Product product = productService.getProductById(id);
        if (product == null) {
            return new ApiResponse<>("error", "Product not found with id: " + id, null);
        }
        return new ApiResponse<>("success", "Product retrieved successfully", product);
    }

    // GET /api/products/search?query=xxx&page=0&size=20
    @GetMapping("/search")
    public ApiResponse<List<Product>> searchProducts(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<Product> results = productService.searchProducts(query, page, size);
        if (results.isEmpty()) {
            return new ApiResponse<>("error", "No products found for query: " + query, null);
        }
        return new ApiResponse<>("success", "Products retrieved successfully", results);
    }
}
