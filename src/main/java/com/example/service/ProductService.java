package com.example.service;

import com.example.dto.CompareRequest;
import com.example.dto.RecommendRequest;
import com.example.model.Product;
import com.example.repository.ProductRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final TrieService trieService;
    private final SearchTrackingService searchTrackingService; // ADD THIS
    private Map<String, Product> productByIdMap = new HashMap<>();
    private Map<String, List<Product>> productsByCategoryMap = new HashMap<>();
    private Map<String, List<Product>> productsByStoreMap = new HashMap<>();

    // NEW: Inverted Index for O(1) word lookup
    // Maps normalized word -> Set of product IDs containing that word
    private Map<String, Set<String>> invertedIndex = new HashMap<>();

    // NEW: PageRank-like scoring for result ranking
    // Maps product ID -> computed ranking score
    private Map<String, Double> pageRankScore = new HashMap<>();

    // Regex pattern for query validation (alphanumeric, spaces, and safe regex tokens)
    private static final Pattern QUERY_VALIDATION_PATTERN =
            Pattern.compile("^[a-zA-Z0-9\\s\\-\\.\\|\\*\\+\\?()]{2,100}$");

    @PostConstruct
    public void initCache() {
        reloadCache();
    }

    /**
     * Algorithm: Cache initialization with Inverted Index & PageRank
     * <p>
     * 1. Clear all existing caches and indexes
     * 2. Load all products from repository
     * 3. For each product:
     * a. Build category and store maps (O(1) lookup)
     * b. Index product names in Trie for autocomplete (O(m) per word)
     * c. Build inverted index: tokenize name, map each word -> product ID
     * d. Compute PageRank score based on:
     * - Deal type bonus (LOYALTY: +10, SALE: +5)
     * - Title length penalty (shorter = better)
     * - Base score: 1.0
     * <p>
     * Time Complexity: O(n*m) where n=products, m=avg words per product
     * Space Complexity: O(n*m) for inverted index storage
     */
    public void reloadCache() {
        productByIdMap.clear();
        productsByCategoryMap.clear();
        productsByStoreMap.clear();
        invertedIndex.clear();
        pageRankScore.clear();
        trieService.clear();

        List<Product> products = productRepository.findAll();
        for (Product p : products) {
            productByIdMap.put(p.getId(), p);
            productsByCategoryMap.computeIfAbsent(p.getCategory(), k -> new ArrayList<>()).add(p);
            productsByStoreMap.computeIfAbsent(p.getStoreName(), k -> new ArrayList<>()).add(p);

            // Index product names in Trie for autocomplete
            trieService.insert(p.getProductName());

            // Build inverted index: word -> product IDs
            String[] words = p.getProductName().toLowerCase().split("\\s+");
            for (String word : words) {
                if (word.length() > 2) {
                    trieService.insert(word);

                    // Add to inverted index
                    String normalizedWord = normalizeWord(word);
                    invertedIndex.computeIfAbsent(normalizedWord, k -> new HashSet<>())
                            .add(p.getId());
                }
            }

            // Compute PageRank score for this product
            pageRankScore.put(p.getId(), computePageRank(p));
        }
    }

    /**
     * Algorithm: Query Validation using Whitelist Regex
     * <p>
     * Purpose: Prevent SQL injection, XSS, and invalid regex patterns
     * <p>
     * Validation Rules:
     * 1. Length: 2-100 characters
     * 2. Allowed characters: a-z, A-Z, 0-9, spaces, hyphens, periods
     * 3. Safe regex tokens: | * + ? ( ) for pattern matching
     * 4. Blocks: quotes, semicolons, slashes, angle brackets, special chars
     * <p>
     * Time Complexity: O(n) where n=query length
     *
     * @param query The search query to validate
     * @return true if query passes validation, false otherwise
     */
    private boolean isValidQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return false;
        }

        // Check against whitelist pattern
        return QUERY_VALIDATION_PATTERN.matcher(query.trim()).matches();
    }

    /**
     * Algorithm: PageRank Score Computation
     * <p>
     * Scoring Formula:
     * score = baseScore + dealBonus - lengthPenalty
     * <p>
     * Where:
     * - baseScore = 1.0 (all products start equal)
     * - dealBonus = 10.0 (LOYALTY) | 5.0 (SALE/PROMO) | 0.0 (NONE)
     * - lengthPenalty = titleLength / 50.0 (shorter titles rank higher)
     * <p>
     * Rationale:
     * - Products with deals are more relevant to price-conscious users
     * - Shorter names are often more popular/generic products
     * - Loyalty deals get highest boost (exclusive value)
     * <p>
     * Time Complexity: O(1)
     */
    private double computePageRank(Product product) {
        double score = 1.0; // Base score

        // Deal type bonus
        if (product.getDealType() != null) {
            switch (product.getDealType().toUpperCase()) {
                case "LOYALTY":
                    score += 10.0;
                    break;
                case "SALE":
                case "PROMO":
                    score += 5.0;
                    break;
                default:
                    break;
            }
        }

        // Title length penalty (shorter = better, more generic/popular)
        int titleLength = product.getProductName().length();
        score -= titleLength / 50.0;

        return Math.max(score, 0.1); // Minimum score of 0.1
    }

    /**
     * Algorithm: Normalize word for inverted index
     * Removes punctuation, converts to lowercase, trims whitespace
     */
    private String normalizeWord(String word) {
        return word.toLowerCase()
                .replaceAll("[^a-z0-9]", "")
                .trim();
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


    /**
     * Levenshtein Distance Algorithm (Edit Distance)
     * <p>
     * Calculates minimum number of single-character edits needed to transform
     * one string into another.
     * <p>
     * Operations counted:
     * 1. Insertion (add character)
     * 2. Deletion (remove character)
     * 3. Substitution (replace character)
     * <p>
     * Algorithm (Dynamic Programming):
     * - Build matrix dp[i][j] = edit distance between first i chars of s1
     * and first j chars of s2
     * - dp[0][j] = j (insert j characters)
     * - dp[i][0] = i (delete i characters)
     * - dp[i][j] = min of:
     * - dp[i-1][j] + 1 (delete from s1)
     * - dp[i][j-1] + 1 (insert into s1)
     * - dp[i-1][j-1] + cost (substitute if chars differ)
     * <p>
     * Example:
     * "orgnic" → "organic" = 1 (insert 'a')
     * "banan" → "banana" = 1 (insert 'a')
     * "milkk" → "milk" = 1 (delete 'k')
     * <p>
     * Time Complexity: O(m*n) where m, n are string lengths
     * Space Complexity: O(m*n) for dp matrix
     */
    private int levenshteinDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();

        // Create DP table
        int[][] dp = new int[m + 1][n + 1];

        // Initialize base cases
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i; // Delete all chars from s1
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j; // Insert all chars into s1
        }

        // Fill DP table
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    // Characters match, no operation needed
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    // Take minimum of three operations
                    dp[i][j] = 1 + Math.min(
                            Math.min(
                                    dp[i - 1][j],     // Delete
                                    dp[i][j - 1]      // Insert
                            ),
                            dp[i - 1][j - 1]      // Substitute
                    );
                }
            }
        }

        return dp[m][n];
    }

    /**
     * Get spelling suggestions for a misspelled query word
     * <p>
     * Algorithm:
     * 1. Get dictionary from inverted index keys (all indexed words)
     * 2. For each dictionary word:
     * - Calculate Levenshtein distance
     * - If distance <= 2, consider it a potential correction
     * 3. Sort suggestions by distance (closest first)
     * 4. Return top 5 suggestions
     * <p>
     * Distance Thresholds:
     * - distance = 0: exact match
     * - distance = 1: single typo (most common)
     * - distance = 2: double typo or transposition
     * - distance > 2: probably different word
     * <p>
     * Time Complexity: O(d*m*n) where d=dictionary size, m,n=word lengths
     */
    public List<String> getSpellingSuggestions(String word) {
        if (word == null || word.isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedWord = normalizeWord(word);
        Set<String> dictionary = invertedIndex.keySet();

        // Store word with its distance
        List<Map.Entry<String, Integer>> candidates = new ArrayList<>();

        for (String dictWord : dictionary) {
            int distance = levenshteinDistance(normalizedWord, dictWord);

            // Only consider words within edit distance 2
            if (distance > 0 && distance <= 2) {
                candidates.add(new AbstractMap.SimpleEntry<>(dictWord, distance));
            }
        }

        // Sort by distance (closest first), then alphabetically
        candidates.sort((a, b) -> {
            if (!a.getValue().equals(b.getValue())) {
                return Integer.compare(a.getValue(), b.getValue());
            }
            return a.getKey().compareTo(b.getKey());
        });

        // Return top 5 suggestions
        return candidates.stream()
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * MODIFIED: Enhanced Search with Spell Checking
     * <p>
     * NEW Search Pipeline:
     * 1. VALIDATION: Check query against whitelist regex
     * 2. REGEX MATCHING: If contains regex tokens
     * 3. INVERTED INDEX LOOKUP: For keyword searches
     * 4. SPELL CHECK: If no results found, suggest corrections
     * 5. FALLBACK: Boyer-Moore substring search
     * 6. RANKING: Sort by PageRank score
     * <p>
     * Spell Check Logic:
     * - If search returns 0 results
     * - Check each query word for typos
     * - Return suggestions instead of empty list
     */
    public Map<String, Object> searchProductsWithSpellCheck(String query, int page, int size) {
        Map<String, Object> result = new HashMap<>();

        if (query == null || query.isEmpty()) {
            result.put("products", Collections.emptyList());
            result.put("correctedQuery", null);
            result.put("suggestions", Collections.emptyList());
            return result;
        }

        // Step 1: Validate query
        if (!isValidQuery(query)) {
            result.put("products", Collections.emptyList());
            result.put("correctedQuery", null);
            result.put("suggestions", Collections.emptyList());
            return result;
        }
        searchTrackingService.recordSearch(query);
        String normalizedQuery = query.toLowerCase().trim();
        Set<Product> matchedProducts = new HashSet<>();

        // Step 2: Try regex pattern matching
        if (containsRegexTokens(query)) {
            try {
                Pattern regexPattern = Pattern.compile(normalizedQuery, Pattern.CASE_INSENSITIVE);
                for (Product p : productByIdMap.values()) {
                    if (regexPattern.matcher(p.getProductName().toLowerCase()).find()) {
                        matchedProducts.add(p);
                    }
                }
            } catch (PatternSyntaxException e) {
                // Ignore invalid regex
            }
        }

        // Step 3: Use inverted index lookup
        if (matchedProducts.isEmpty()) {
            Set<String> candidateIds = new HashSet<>();
            String[] queryWords = normalizedQuery.split("\\s+");

            boolean indexHit = false;
            for (String word : queryWords) {
                String normalized = normalizeWord(word);
                if (invertedIndex.containsKey(normalized)) {
                    candidateIds.addAll(invertedIndex.get(normalized));
                    indexHit = true;
                }
            }

            if (indexHit) {
                for (String id : candidateIds) {
                    Product p = productByIdMap.get(id);
                    if (p != null) {
                        matchedProducts.add(p);
                    }
                }
            }
        }

        // Step 4: SPELL CHECK - If no results, check for typos
        if (matchedProducts.isEmpty()) {
            String[] queryWords = normalizedQuery.split("\\s+");
            List<String> allSuggestions = new ArrayList<>();
            StringBuilder correctedQuery = new StringBuilder();

            for (String word : queryWords) {
                String normalized = normalizeWord(word);

                // Check if word exists in dictionary
                if (!invertedIndex.containsKey(normalized)) {
                    // Word not found, get suggestions
                    List<String> suggestions = getSpellingSuggestions(word);

                    if (!suggestions.isEmpty()) {
                        // Use best suggestion for auto-correct
                        correctedQuery.append(suggestions.get(0)).append(" ");
                        allSuggestions.addAll(suggestions);
                    } else {
                        correctedQuery.append(word).append(" ");
                    }
                } else {
                    correctedQuery.append(word).append(" ");
                }
            }

            String corrected = correctedQuery.toString().trim();

            // If we found corrections, try searching with corrected query
            if (!corrected.equals(normalizedQuery) && !allSuggestions.isEmpty()) {
                // Recursively search with corrected query
                List<Product> correctedResults = searchProducts(corrected, page, size);

                result.put("products", correctedResults);
                result.put("originalQuery", query);
                result.put("correctedQuery", corrected);
                result.put("suggestions", allSuggestions.stream().distinct().limit(5).collect(Collectors.toList()));
                return result;
            }
        }

        // Step 5: Fallback to Boyer-Moore
        if (matchedProducts.isEmpty()) {
            for (Product p : productByIdMap.values()) {
                if (boyerMooreSearch(p.getProductName().toLowerCase(), normalizedQuery)) {
                    matchedProducts.add(p);
                }
            }
        }

        // Step 6: Rank by PageRank score
        List<Product> rankedResults = matchedProducts.stream()
                .sorted((p1, p2) -> {
                    double score1 = pageRankScore.getOrDefault(p1.getId(), 0.0);
                    double score2 = pageRankScore.getOrDefault(p2.getId(), 0.0);
                    return Double.compare(score2, score1);
                })
                .collect(Collectors.toList());

        // Track query in Trie
        trieService.insert(query);

        // Paginate results
        List<Product> paginatedResults = paginate(rankedResults, page, size);

        result.put("products", paginatedResults);
        result.put("originalQuery", query);
        result.put("correctedQuery", null);
        result.put("suggestions", Collections.emptyList());

        return result;
    }

    /**
     * Algorithm: Enhanced Search with Regex + Inverted Index + PageRank
     * <p>
     * Search Pipeline:
     * 1. VALIDATION: Check query against whitelist regex
     * - Reject if invalid (prevents injection attacks)
     * <p>
     * 2. REGEX MATCHING (if query contains regex tokens):
     * - Try to compile query as regex pattern
     * - Apply pattern matching to all product names
     * - Collect matching products
     * <p>
     * 3. INVERTED INDEX LOOKUP (for simple keyword searches):
     * - Tokenize query into words
     * - Look up each word in inverted index (O(1) per word)
     * - Collect candidate product IDs
     * - Union all candidate sets
     * <p>
     * 4. FALLBACK: Boyer-Moore substring search
     * - If no regex or index hits, scan all products
     * - Use Boyer-Moore for efficient pattern matching
     * <p>
     * 5. RANKING:
     * - Sort results by precomputed PageRank score (descending)
     * - Higher scores = better deals, more relevant products
     * <p>
     * 6. TRACKING:
     * - Add valid query to Trie for autocomplete
     * <p>
     * Time Complexity:
     * - Best case (index hit): O(k + m*log(m)) where k=words, m=results
     * - Worst case (full scan): O(n*p) where n=products, p=pattern length
     */
    public List<Product> searchProducts(String query, int page, int size) {
        if (query == null || query.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 1: Validate query
        if (!isValidQuery(query)) {
            return Collections.emptyList(); // Reject invalid queries
        }
        searchTrackingService.recordSearch(query);
        String normalizedQuery = query.toLowerCase().trim();
        Set<Product> matchedProducts = new HashSet<>();

        // Step 2: Try regex pattern matching first
        if (containsRegexTokens(query)) {
            try {
                Pattern regexPattern = Pattern.compile(normalizedQuery, Pattern.CASE_INSENSITIVE);
                for (Product p : productByIdMap.values()) {
                    if (regexPattern.matcher(p.getProductName().toLowerCase()).find()) {
                        matchedProducts.add(p);
                    }
                }
            } catch (PatternSyntaxException e) {
                // If regex is invalid, fall through to other methods
            }
        }

        // Step 3: Use inverted index for keyword lookup (if no regex matches)
        if (matchedProducts.isEmpty()) {
            Set<String> candidateIds = new HashSet<>();
            String[] queryWords = normalizedQuery.split("\\s+");

            boolean indexHit = false;
            for (String word : queryWords) {
                String normalized = normalizeWord(word);
                if (invertedIndex.containsKey(normalized)) {
                    candidateIds.addAll(invertedIndex.get(normalized));
                    indexHit = true;
                }
            }

            if (indexHit) {
                for (String id : candidateIds) {
                    Product p = productByIdMap.get(id);
                    if (p != null) {
                        matchedProducts.add(p);
                    }
                }
            }
        }

        // Step 4: Fallback to Boyer-Moore substring search
        if (matchedProducts.isEmpty()) {
            for (Product p : productByIdMap.values()) {
                if (boyerMooreSearch(p.getProductName().toLowerCase(), normalizedQuery)) {
                    matchedProducts.add(p);
                }
            }
        }

        // Step 5: Rank by PageRank score (highest first)
        List<Product> rankedResults = matchedProducts.stream()
                .sorted((p1, p2) -> {
                    double score1 = pageRankScore.getOrDefault(p1.getId(), 0.0);
                    double score2 = pageRankScore.getOrDefault(p2.getId(), 0.0);
                    return Double.compare(score2, score1); // Descending order
                })
                .collect(Collectors.toList());

        // Step 6: Track query in Trie for autocomplete
        trieService.insert(query);

        return paginate(rankedResults, page, size);
    }

    /**
     * Check if query contains regex special tokens
     */
    private boolean containsRegexTokens(String query) {
        return query.matches(".*[|*+?()\\[\\]{}^$\\\\].*");
    }

    /**
     * Boyer-Moore Algorithm: Efficient substring pattern matching
     * <p>
     * Algorithm Steps:
     * 1. Build bad character table (HashMap for Unicode support)
     * - Maps each character in pattern to its last occurrence index
     * <p>
     * 2. Align pattern with text from left
     * 3. Compare pattern from RIGHT to LEFT
     * 4. On mismatch:
     * - Use bad character rule to skip alignments
     * - Shift pattern based on rightmost occurrence of mismatched char
     * <p>
     * 5. Return true on first match found
     * <p>
     * Time Complexity:
     * - Best case: O(n/m) with large skips
     * - Worst case: O(n*m) with many matches
     * - Average case: O(n) much better than naive O(n*m)
     * <p>
     * Space Complexity: O(m) for bad character table
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

    /**
     * Algorithm: Compare Products with Sale Filter
     * <p>
     * Multi-stage filtering pipeline:
     * 1. Category filter (exact match, case-insensitive)
     * 2. Availability filter (only "In-stock" if required)
     * 3. Store filter (match selected stores)
     * 4. Sale-only filter (NEW - filter by dealType)
     * 5. Price range filter (use sale price if available)
     * 6. Sort by effective price (ascending - cheapest first)
     * <p>
     * Time Complexity: O(n*log(n)) where n=products (dominated by sorting)
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
                .filter(p -> !req.isSaleOnly() ||
                        (p.getDealType() != null && !p.getDealType().equalsIgnoreCase("NONE")))
                .filter(p -> {
                    if (req.getPriceRange() == null) return true;
                    double price;
                    try {
                        String priceStr = (p.getSalePrice() != null && !p.getSalePrice().isBlank())
                                ? p.getSalePrice() : p.getPrice();
                        price = Double.parseDouble(priceStr.replace("$", ""));
                    } catch (Exception e) {
                        return false;
                    }
                    return price >= req.getPriceRange().get(0)
                            && price <= req.getPriceRange().get(1);
                })
                .sorted(Comparator.comparingDouble(p -> {
                    try {
                        String priceStr = (p.getSalePrice() != null && !p.getSalePrice().isBlank())
                                ? p.getSalePrice() : p.getPrice();
                        return Double.parseDouble(priceStr.replace("$", ""));
                    } catch (Exception e) {
                        return Double.MAX_VALUE;
                    }
                }))
                .toList();
    }

    /**
     * Algorithm: Store Recommendation with Weighted Scoring
     * <p>
     * Scoring System:
     * 1. Product match: +5 points (if product name matches user needs)
     * 2. Preferred store: +3 points (loyalty to specific store)
     * 3. Loyalty program: +2 points (if product has loyalty deal)
     * 4. Price advantage: -price/10 points (cheaper = higher score)
     * <p>
     * Aggregation:
     * - Sum scores across all products per store
     * - Rank stores by total score (descending)
     * <p>
     * Time Complexity: O(n + s*log(s)) where n=products, s=stores
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

            if (req.getProductNeeds().stream()
                    .anyMatch(name -> p.getProductName().toLowerCase().contains(name.toLowerCase())))
                score += 5;

            if (req.getPreferredProvider() != null &&
                    p.getStoreName().equalsIgnoreCase(req.getPreferredProvider()))
                score += 3;

            if (req.isLoyaltyProgram() && "LOYALTY".equalsIgnoreCase(p.getDealType()))
                score += 2;

            score -= basePrice / 10.0;

            storeScores.put(p.getStoreName(),
                    storeScores.getOrDefault(p.getStoreName(), 0.0) + score);
        }

        return storeScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Algorithm: Rank Stores by Keyword Occurrence (Boyer-Moore + Max-Heap)
     * <p>
     * Purpose: Find which stores have the most products matching a keyword
     * <p>
     * Steps:
     * 1. COUNTING PHASE:
     * - For each product:
     * a. Apply Boyer-Moore search on product name
     * b. Count total keyword occurrences (not just presence)
     * - Aggregate counts per store
     * <p>
     * 2. RANKING PHASE (Max-Heap):
     * - Use PriorityQueue with reverse comparator (max-heap behavior)
     * - Heap property: store with highest count at root
     * - Insert all stores: O(s*log(s)) where s=number of stores
     * - Extract in descending order
     * <p>
     * 3. RESULT FORMATTING:
     * - Build response with rank, store name, and occurrence count
     * <p>
     * Boyer-Moore Advantages:
     * - Skips unnecessary character comparisons
     * - Best case: O(n/m) where n=text length, m=pattern length
     * - Average case: O(n) much better than naive O(n*m)
     * <p>
     * Max-Heap Advantages:
     * - Efficient priority-based extraction: O(log n)
     * - Natural ordering for ranking problems
     * - Can be extended to "top-k" stores easily
     * <p>
     * Time Complexity: O(p*n + s*log(s)) where:
     * - p = number of products
     * - n = average product name length
     * - s = number of unique stores
     * <p>
     * Space Complexity: O(s) for store occurrence map and heap
     *
     * @param keyword The keyword to search for in product names
     * @return List of maps containing rank, storeName, and occurrences
     */
    public List<Map<String, Object>> rankStoresByKeyword(String keyword) {
        String normalizedKeyword = keyword.toLowerCase().trim();

        // Step 1: Count keyword occurrences per store using Boyer-Moore
        Map<String, Integer> storeOccurrences = new HashMap<>();

        for (Product product : productByIdMap.values()) {
            String storeName = product.getStoreName();
            String productName = product.getProductName().toLowerCase();

            // Count occurrences using Boyer-Moore
            int count = boyerMooreCountOccurrences(productName, normalizedKeyword);

            if (count > 0) {
                storeOccurrences.put(storeName,
                        storeOccurrences.getOrDefault(storeName, 0) + count);
            }
        }

        // Step 2: Use Max-Heap (PriorityQueue) to rank stores
        // PriorityQueue is min-heap by default, so reverse comparator for max-heap
        PriorityQueue<Map.Entry<String, Integer>> maxHeap = new PriorityQueue<>(
                (a, b) -> b.getValue().compareTo(a.getValue()) // Descending order
        );

        maxHeap.addAll(storeOccurrences.entrySet());

        // Step 3: Extract ranked results from heap
        List<Map<String, Object>> rankedStores = new ArrayList<>();
        int rank = 1;

        while (!maxHeap.isEmpty()) {
            Map.Entry<String, Integer> entry = maxHeap.poll();

            Map<String, Object> storeData = new HashMap<>();
            storeData.put("rank", rank++);
            storeData.put("storeName", entry.getKey());
            storeData.put("occurrences", entry.getValue());

            rankedStores.add(storeData);
        }

        return rankedStores;
    }

    /**
     * Boyer-Moore Count Occurrences - Counts ALL pattern matches in text
     * <p>
     * Algorithm:
     * 1. Build bad character table (HashMap for Unicode support)
     * - Maps each character in pattern to its rightmost position
     * <p>
     * 2. Alignment and matching:
     * - Start with shift = 0 (align pattern at start of text)
     * - For each alignment:
     * a. Compare pattern RIGHT-TO-LEFT
     * b. If full match: increment count, shift by 1 (find overlaps)
     * c. If mismatch: use bad character rule to skip alignments
     * <p>
     * 3. Bad Character Rule:
     * - On mismatch at position j:
     * - Look up mismatched text character in pattern
     * - Shift pattern to align last occurrence of that character
     * - If character not in pattern, shift past it entirely
     * - Always shift at least 1 position forward
     * <p>
     * Example (counting overlaps):
     * Text:    "banana"
     * Pattern: "ana"
     * Result:  2 matches (at index 1 and 3)
     * <p>
     * Why Count All Occurrences?
     * - For ranking: store with 10 "organic" mentions > store with 2
     * - More accurate than binary presence/absence
     * - Reflects product diversity within category
     * <p>
     * Time Complexity:
     * - Best case: O(n/m) with large skips
     * - Worst case: O(n*m) with many matches
     * - Average case: O(n) much better than naive search
     * <p>
     * Space Complexity: O(m) for bad character table
     *
     * @param text    The text to search in (product name)
     * @param pattern The pattern to search for (keyword)
     * @return Number of occurrences found
     */
    private int boyerMooreCountOccurrences(String text, String pattern) {
        int n = text.length();
        int m = pattern.length();

        if (m == 0 || m > n) {
            return 0;
        }

        // Build bad character table using HashMap for Unicode safety
        Map<Character, Integer> badChar = new HashMap<>();
        for (int i = 0; i < m; i++) {
            badChar.put(pattern.charAt(i), i);
        }

        int shift = 0;
        int count = 0;

        while (shift <= (n - m)) {
            int j = m - 1;

            // Compare pattern from right to left
            while (j >= 0 && pattern.charAt(j) == text.charAt(shift + j)) {
                j--;
            }

            if (j < 0) {
                // Match found - increment count
                count++;

                // Shift by 1 to find overlapping matches
                // Example: "aaa" in "aaaa" finds matches at index 0 and 1
                shift += 1;
            } else {
                // Use bad character rule for efficient shifting
                char badCharInText = text.charAt(shift + j);
                int lastOccur = badChar.getOrDefault(badCharInText, -1);
                shift += Math.max(1, j - lastOccur);
            }
        }

        return count;
    }
}