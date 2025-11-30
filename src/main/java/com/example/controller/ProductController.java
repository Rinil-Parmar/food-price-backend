package com.example.controller;

import com.example.model.ApiResponse;
import com.example.model.Product;
import com.example.service.ProductService;
import com.example.service.SearchTrackingService;
import com.example.service.TrieService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final SearchTrackingService searchTrackingService;
    private final TrieService trieService;

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
//    @GetMapping("/search")
//    public ApiResponse<List<Product>> searchProducts(
//            @RequestParam String query,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size) {
//
//        if (query == null || query.trim().isEmpty()) {
//            return new ApiResponse<>("error", "Search query cannot be empty", null);
//        }
//
//        // Track search frequency
//        searchTrackingService.recordSearch(query);
//
//        // Add word to Trie for autocomplete ranking
//        trieService.insert(query);
//
//        List<Product> results = productService.searchProducts(query, page, size);
//
//        if (results.isEmpty()) {
//            return new ApiResponse<>("error", "No products found for query: " + query, null);
//        }
//
//        return new ApiResponse<>("success", "Products retrieved successfully", results);
//    }

    /**
     * Search Products API
     * <p>
     * Endpoint: GET /api/search?query=milk&page=0&size=20
     * <p>
     * Features:
     * - Regex pattern matching (e.g., organic\s+eggs, milk.*2L, apple|banana)
     * - Inverted index lookup for fast keyword search
     * - Boyer-Moore substring matching fallback
     * - PageRank-based result ranking
     *
     * @param query Search query (2-100 characters)
     * @param page  Page number (default: 0)
     * @param size  Page size (default: 20)
     * @return API response with ranked product list
     */
//    @GetMapping("/search")
//    public ApiResponse<List<Product>> searchProducts(
//            @RequestParam String query,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size) {
//
//        // Validate query presence
//        if (query == null || query.trim().isEmpty()) {
//            return new ApiResponse<>("error",
//                    "Search query cannot be empty",
//                    Collections.emptyList());
//        }
//
//        // Validate query length
//        if (query.length() < 2 || query.length() > 100) {
//            return new ApiResponse<>("error",
//                    "Query must be between 2-100 characters",
//                    Collections.emptyList());
//        }
//
//        // Delegate to service layer
//        List<Product> results = productService.searchProducts(query, page, size);
//
//        if (results.isEmpty()) {
//            return new ApiResponse<>("success",
//                    "No products found for query: " + query,
//                    results);
//        }
//
//        return new ApiResponse<>("success",
//                results.size() + " products found",
//                results);
//    }

    /**
     * Enhanced Search API with Spell Checking
     * <p>
     * Features:
     * - Auto-corrects typos using Levenshtein distance
     * - Shows "Did you mean?" suggestions
     * - Returns corrected results automatically
     * <p>
     * Response includes:
     * - products: List of matching products
     * - originalQuery: What user typed
     * - correctedQuery: Auto-corrected query (if typo detected)
     * - suggestions: Alternative spelling suggestions
     */
    @GetMapping("/search")
    public ApiResponse<Map<String, Object>> searchProducts(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (query == null || query.trim().isEmpty()) {
            return new ApiResponse<>("error",
                    "Search query cannot be empty",
                    null);
        }

        if (query.length() < 2 || query.length() > 100) {
            return new ApiResponse<>("error",
                    "Query must be between 2-100 characters",
                    null);
        }

        // Get search results with spell check
        Map<String, Object> searchResult = productService.searchProductsWithSpellCheck(query, page, size);

        @SuppressWarnings("unchecked")
        List<Product> products = (List<Product>) searchResult.get("products");
        String correctedQuery = (String) searchResult.get("correctedQuery");

        // Build response message
        String message;
        if (correctedQuery != null) {
            message = "Showing results for '" + correctedQuery + "' (corrected from '" + query + "')";
        } else if (products.isEmpty()) {
            message = "No products found for query: " + query;
        } else {
            message = products.size() + " products found";
        }

        return new ApiResponse<>("success", message, searchResult);
    }

    @GetMapping("/search/ranking")
    public ApiResponse<?> getTopSearches(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return new ApiResponse<>(
                "success",
                "Top search keywords",
                searchTrackingService.getTopKeywords(limit)
        );
    }


    @GetMapping("/search/frequency")
    public ApiResponse<?> getSearchFrequency(@RequestParam String keyword) {
        long freq = searchTrackingService.getKeywordFrequency(keyword);
        return new ApiResponse<>(
                "success",
                "Keyword search frequency",
                freq
        );
    }


}
