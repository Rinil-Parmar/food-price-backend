package com.example.service;

import com.example.model.Product;
import com.example.repository.ProductRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    private Map<String, Product> productByIdMap = new HashMap<>();
    private Map<String, List<Product>> productsByCategoryMap = new HashMap<>();
    private Map<String, List<Product>> productsByStoreMap = new HashMap<>();

    @PostConstruct
    public void initCache() {
        reloadCache();
    }

    public void reloadCache() {
        productByIdMap.clear();
        productsByCategoryMap.clear();
        productsByStoreMap.clear();

        List<Product> products = productRepository.findAll();
        for (Product p : products) {
            productByIdMap.put(p.getId(), p);
            productsByCategoryMap.computeIfAbsent(p.getCategory(), k -> new ArrayList<>()).add(p);
            productsByStoreMap.computeIfAbsent(p.getStoreName(), k -> new ArrayList<>()).add(p);
        }
    }

    // -----------------------------
    // Pagination Helper
    // -----------------------------
    private <T> List<T> paginate(List<T> items, int page, int size) {
        int total = items.size();
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, total);

        if (fromIndex >= total) {
            return Collections.emptyList();
        }
        return items.subList(fromIndex, toIndex);
    }

    // -----------------------------
    // GET APIs with Pagination
    // -----------------------------
    public List<Product> getAllProducts(int page, int size) {
        return paginate(new ArrayList<>(productByIdMap.values()), page, size);
    }

    public List<Product> getProductsByCategory(String category, int page, int size) {
        return paginate(productsByCategoryMap.getOrDefault(category, Collections.emptyList()), page, size);
    }

    public List<Product> getProductsByStore(String storeName, int page, int size) {
        return paginate(productsByStoreMap.getOrDefault(storeName, Collections.emptyList()), page, size);
    }

    public Product getProductById(String id) {
        return productByIdMap.get(id);
    }

    // -----------------------------
    // Search API with Pagination
    // -----------------------------
    public List<Product> searchProducts(String query, int page, int size) {
        if (query == null || query.isEmpty()) return Collections.emptyList();

        List<Product> allProducts = new ArrayList<>(productByIdMap.values());
        List<Product> matched = allProducts.stream()
//                .filter(p -> p.getProductName().toLowerCase().contains(query.toLowerCase()))
                .filter(p -> boyerMooreSearch(p.getProductName().toLowerCase(), query.toLowerCase()))
                .collect(Collectors.toList());

        return paginate(matched, page, size);
    }

    /**
     * Boyer–Moore substring search algorithm (Unicode-safe version).
     * Uses HashMap instead of fixed-size array for bad character table
     * to support all Unicode characters.
     */
    private boolean boyerMooreSearch(String text, String pattern) {
        int n = text.length();
        int m = pattern.length();
        if (m == 0) return true;
        if (m > n) return false;

        // Build bad character table using HashMap for Unicode safety
        Map<Character, Integer> badChar = new HashMap<>();
        for (int i = 0; i < m; i++) {
            badChar.put(pattern.charAt(i), i);
        }

        int shift = 0;
        while (shift <= (n - m)) {
            int j = m - 1;

            // Compare pattern from end to start
            while (j >= 0 && pattern.charAt(j) == text.charAt(shift + j)) {
                j--;
            }

            // If match found
            if (j < 0) {
                return true;
            } else {
                // Use bad character rule with safe lookup
                char badCharInText = text.charAt(shift + j);
                int lastOccur = badChar.getOrDefault(badCharInText, -1);
                shift += Math.max(1, j - lastOccur);
            }
        }
        return false;
    }

}