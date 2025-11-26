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
public class SearchController {

    private final ProductService productService;
    private final TrieService trieService;
    private final SearchTrackingService searchTrackingService;

    /**
     * Search Products API - Search products by query with pagination.
     * <p>
     * Endpoint: GET /api/products/search?query=milk&page=0&size=20
     * <p>
     * Algorithm:
     * 1. Receive search query from request parameter
     * 2. Use Boyer-Moore algorithm for efficient substring matching
     * 3. Track search query in Trie for autocomplete suggestions
     * 4. Return paginated results
     * <p>
     * Time Complexity: O(n*m) where n=products, m=pattern length
     */
    @GetMapping("/search/trie")
    public ApiResponse<List<Product>> searchProducts(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (query == null || query.trim().isEmpty()) {
            return new ApiResponse<>("error", "Search query cannot be empty", List.of());
        }

        List<Product> results = productService.searchProducts(query, page, size);

        if (results.isEmpty()) {
            return new ApiResponse<>("success", "No products found for query: " + query, results);
        }

        return new ApiResponse<>("success", "Products retrieved successfully", results);
    }

    /**
     * Autocomplete API - Returns search suggestions based on prefix.
     * <p>
     * Endpoint: GET /api/products/search/autocomplete?prefix=mil
     * <p>
     * Example Response:
     * {
     * "status": "success",
     * "message": "Suggestions found",
     * "data": ["milk", "milano cookies", "milky way"]
     * }
     * <p>
     * Algorithm:
     * 1. Receive prefix from query parameter
     * 2. Use Trie to find all words starting with prefix
     * 3. Rank suggestions by search frequency (most popular first)
     * 4. Return top 10 most relevant suggestions
     * <p>
     * Time Complexity: O(m + n*k) where m=prefix length, n=results, k=avg word length
     */
    @GetMapping("/search/autocomplete")
    public ApiResponse<List<String>> autocomplete(@RequestParam String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return new ApiResponse<>("error", "Prefix cannot be empty", List.of());
        }

        List<String> suggestions = trieService.autocomplete(prefix);

        if (suggestions.isEmpty()) {
            return new ApiResponse<>("success", "No suggestions found", suggestions);
        }

        return new ApiResponse<>("success", "Suggestions found", suggestions);
    }

    /**
     * Store Ranking API
     * <p>
     * Endpoint: GET /api/products/rank/stores?keyword=organic
     * <p>
     * Ranks stores by keyword occurrence count using:
     * - Boyer-Moore algorithm for pattern matching
     * - Max-Heap (PriorityQueue) for efficient ranking
     * <p>
     * Response Format:
     * [
     * {"rank": 1, "storeName": "Whole Foods", "occurrences": 45},
     * {"rank": 2, "storeName": "Trader Joe's", "occurrences": 32}
     * ]
     * <p>
     * Use Cases:
     * - Find stores with most "organic" products
     * - Compare store inventory for specific keywords
     * - Identify specialty stores (gluten-free, vegan, etc.)
     *
     * @param keyword Keyword to search for (2-50 characters)
     * @return Ranked list of stores with occurrence counts
     */
    @GetMapping("/rank/stores")
    public ApiResponse<List<Map<String, Object>>> rankStoresByKeyword(
            @RequestParam String keyword) {

        // Validate keyword presence
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ApiResponse<>("error",
                    "Keyword cannot be empty",
                    Collections.emptyList());
        }

        // Validate keyword length
        if (keyword.length() < 2 || keyword.length() > 50) {
            return new ApiResponse<>("error",
                    "Keyword must be between 2-50 characters",
                    Collections.emptyList());
        }

        // Delegate to service layer
        List<Map<String, Object>> rankedStores =
                productService.rankStoresByKeyword(keyword);

        if (rankedStores.isEmpty()) {
            return new ApiResponse<>("success",
                    "No stores found with keyword: " + keyword,
                    rankedStores);
        }

        return new ApiResponse<>("success",
                "Ranked " + rankedStores.size() + " stores by keyword '" + keyword + "'",
                rankedStores);
    }

}