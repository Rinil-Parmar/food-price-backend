package com.example.service;

import com.example.model.Product;
import com.example.repository.ProductRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    // In-memory caches
    private Map<String, Product> productByIdMap = new HashMap<>();
    private Map<String, List<Product>> productsByCategoryMap = new HashMap<>();
    private Map<String, List<Product>> productsByStoreMap = new HashMap<>();

    // Load cache at startup
    @PostConstruct
    public void initCache() {
        reloadCache();
    }

    // Reload cache (call after CSV import)
    public void reloadCache() {
        productByIdMap.clear();
        productsByCategoryMap.clear();
        productsByStoreMap.clear();

        List<Product> products = productRepository.findAll();
        for (Product p : products) {
            // By ID
            productByIdMap.put(p.getId(), p);

            // By Category
            productsByCategoryMap.computeIfAbsent(p.getCategory(), k -> new ArrayList<>()).add(p);

            // By Store
            productsByStoreMap.computeIfAbsent(p.getStoreName(), k -> new ArrayList<>()).add(p);
        }
    }

    // Get all products
    public List<Product> getAllProducts() {
        return new ArrayList<>(productByIdMap.values());
    }

    // Get products by category
    public List<Product> getProductsByCategory(String category) {
        return productsByCategoryMap.getOrDefault(category, Collections.emptyList());
    }

    // Get products by store
    public List<Product> getProductsByStore(String storeName) {
        return productsByStoreMap.getOrDefault(storeName, Collections.emptyList());
    }

    // Get product by ID
    public Product getProductById(String id) {
        return productByIdMap.get(id);
    }
}
