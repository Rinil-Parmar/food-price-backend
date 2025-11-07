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

    @GetMapping
    public ApiResponse<List<Product>> getAllProducts() {
        return new ApiResponse<>("success", "Products retrieved successfully", productService.getAllProducts());
    }

    @GetMapping("/category/{category}")
    public ApiResponse<List<Product>> getProductsByCategory(@PathVariable String category) {
        List<Product> products = productService.getProductsByCategory(category);
        if (products.isEmpty()) {
            return new ApiResponse<>("error", "No products found for category: " + category, null);
        }
        return new ApiResponse<>("success", "Products retrieved successfully", products);
    }

    @GetMapping("/store/{storeName}")
    public ApiResponse<List<Product>> getProductsByStore(@PathVariable String storeName) {
        List<Product> products = productService.getProductsByStore(storeName);
        if (products.isEmpty()) {
            return new ApiResponse<>("error", "No products found for store: " + storeName, null);
        }
        return new ApiResponse<>("success", "Products retrieved successfully", products);
    }

    @GetMapping("/{id}")
    public ApiResponse<Product> getProductById(@PathVariable String id) {
        Product product = productService.getProductById(id);
        if (product == null) {
            return new ApiResponse<>("error", "Product not found with id: " + id, null);
        }
        return new ApiResponse<>("success", "Product retrieved successfully", product);
    }
}