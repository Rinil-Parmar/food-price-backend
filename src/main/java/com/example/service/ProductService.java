package com.example.service;

import com.example.dto.CompareRequest;
import com.example.dto.RecommendRequest;
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
    private final TrieService trieService; // NEW: Inject TrieService

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
        trieService.clear(); // NEW: Clear Trie

        List<Product> products = productRepository.findAll();
        for (Product p : products) {
            productByIdMap.put(p.getId(), p);
            productsByCategoryMap.computeIfAbsent(p.getCategory(), k -> new ArrayList<>()).add(p);
            productsByStoreMap.computeIfAbsent(p.getStoreName(), k -> new ArrayList<>()).add(p);

            // NEW: Index product names in Trie for autocomplete
            trieService.insert(p.getProductName());

            // Optional: Also index individual words from product names
            String[] words = p.getProductName().split("\\s+");
            for (String word : words) {
                if (word.length() > 2) { // Only index words longer than 2 chars
                    trieService.insert(word);
                }
            }
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
    // Search API
    // -----------------------------
    public List<Product> searchProducts(String query, int page, int size) {
        if (query == null || query.isEmpty()) return Collections.emptyList();

        // NEW: Track search query in Trie for autocomplete suggestions
        trieService.insert(query);

        List<Product> allProducts = new ArrayList<>(productByIdMap.values());
        List<Product> matched = allProducts.stream()
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

// =======================================
// Compare Products Across Multiple Stores
// =======================================

    /**
     * Algorithm:
     * 1. Fetch all products from MongoDB.
     * 2. Apply filters:
     * - Category match (if provided)
     * - Availability (only "In-stock" if required)
     * - Store filter (match selected stores)
     * - Price range filter (within min–max range)
     * 3. Normalize price by using salePrice if available, else regular price.
     * 4. Sort filtered products by effective price (ascending).
     * 5. Return the sorted list — lowest price and best deals first.
     */
    public List<Product> compareProducts(CompareRequest req) {

        return productRepository.findAll().stream()

                .filter(p -> req.getCategory() == null ||
                        p.getCategory().equalsIgnoreCase(req.getCategory()))

                .filter(p -> !req.isAvailability() ||
                        "In-stock".equalsIgnoreCase(p.getAvailability()))

                .filter(p -> req.getStores() == null ||
                        req.getStores().isEmpty() ||
                        req.getStores().contains(p.getStoreName()))

                // NEW — sale-only filter using dealType
                .filter(p -> !req.isSaleOnly() ||
                        (p.getDealType() != null && !p.getDealType().equalsIgnoreCase("NONE")))

                .filter(p -> {
                    if (req.getPriceRange() == null) return true;

                    double price;
                    try {
                        String priceStr =
                                (p.getSalePrice() != null && !p.getSalePrice().isBlank())
                                        ? p.getSalePrice()
                                        : p.getPrice();

                        price = Double.parseDouble(priceStr.replace("$", ""));
                    } catch (Exception e) {
                        return false;
                    }

                    return price >= req.getPriceRange().get(0)
                            && price <= req.getPriceRange().get(1);
                })

                .sorted(Comparator.comparingDouble(p -> {
                    try {
                        String priceStr =
                                (p.getSalePrice() != null && !p.getSalePrice().isBlank())
                                        ? p.getSalePrice()
                                        : p.getPrice();

                        return Double.parseDouble(priceStr.replace("$", ""));
                    } catch (Exception e) {
                        return Double.MAX_VALUE;
                    }
                }))

                .toList();
    }


    /**
     * Recommend the best supermarket(s) based on user preferences.
     * <p>
     * Algorithm:
     * - Fetch all products from MongoDB.
     * - For each product, calculate a weighted score using:
     * - Product match with user needs
     * - Preferred provider bonus
     * - Loyalty program bonus
     * - Lower price advantage
     * - Aggregate and rank stores by total score.
     *
     * @param req Recommendation request (preferences, product needs, etc.)
     * @return List of store names ranked by recommendation score.
     */
    public List<String> recommendStores(RecommendRequest req) {
        List<Product> products = productRepository.findAll();
        Map<String, Double> storeScores = new HashMap<>();

        for (Product p : products) {
            double basePrice = 0.0;
            try {
                basePrice = Double.parseDouble(
                        (p.getSalePrice() != null && !p.getSalePrice().isEmpty())
                                ? p.getSalePrice().replace("$", "").trim()
                                : p.getPrice().replace("$", "").trim()
                );
            } catch (Exception ignored) {
            }

            double score = 0.0;

            // Match product needs
            if (req.getProductNeeds().stream()
                    .anyMatch(name -> p.getProductName().toLowerCase().contains(name.toLowerCase())))
                score += 5;

            // Preferred store bonus
            if (req.getPreferredProvider() != null &&
                    p.getStoreName().equalsIgnoreCase(req.getPreferredProvider()))
                score += 3;

            // Loyalty program bonus
            if (req.isLoyaltyProgram() && "LOYALTY".equalsIgnoreCase(p.getDealType()))
                score += 2;

            // Cheaper price gets higher score
            score -= basePrice / 10.0;

            storeScores.put(p.getStoreName(),
                    storeScores.getOrDefault(p.getStoreName(), 0.0) + score);
        }

        // Rank stores by total score
        return storeScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();
    }
}