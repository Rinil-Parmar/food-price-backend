package com.example.service;

import com.example.model.Product;
import com.example.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.PriorityQueue;

@Service
@RequiredArgsConstructor
public class DealService {

    private final ProductRepository productRepository;
    private final Random random = new Random();

    /**
     * Assign random sale and loyalty prices to some products
     * and update them in MongoDB.
     */
    public String assignRandomDeals() {
        List<Product> products = productRepository.findAll();

        if (products.isEmpty()) {
            return "No products found in the database.";
        }

        for (Product product : products) {
            // 40% chance to assign a deal
            if (random.nextDouble() < 0.4) {
                try {
                    double basePrice = Double.parseDouble(product.getPrice().replace("$", "").trim());

                    double salePrice = basePrice * (0.8 + (0.1 * random.nextDouble())); // 10–20% discount
                    double loyaltyPrice = salePrice * 0.95; // Extra 5% off for loyalty

                    product.setSalePrice(String.format("%.2f", salePrice));
                    product.setLoyaltyPrice(String.format("%.2f", loyaltyPrice));

                    // Random deal type
                    String[] dealTypes = {"SALE", "LOYALTY", "CLEARANCE"};
                    product.setDealType(dealTypes[random.nextInt(dealTypes.length)]);
                } catch (Exception e) {
                    // skip invalid price format
                    product.setSalePrice(null);
                    product.setLoyaltyPrice(null);
                    product.setDealType(null);
                }
            } else {
                // No deal → same price for all fields
                product.setSalePrice(product.getPrice());
                product.setLoyaltyPrice(product.getPrice());
                product.setDealType("NONE");
            }

            productRepository.save(product);
        }

        return "Random deals assigned successfully to " + products.size() + " products.";
    }

    /**
     * Get Top N Deals
     * <p>
     * This method retrieves all products from MongoDB, calculates each product's
     * discount percentage based on its price and sale price, and uses a Max-Heap
     * (PriorityQueue) to efficiently extract the top N products with the highest
     * discount values.
     * <p>
     * Algorithm:
     * - For each product, compute: discount% = ((price - salePrice) / price) * 100
     * - Store valid deals (where dealType != NONE) in a Max-Heap ordered by discount%
     * - Extract the top N entries (highest discounts first)
     * <p>
     * Time Complexity: O(n log k)
     * where n = total products, k = limit (top deals to fetch)
     */
    public List<Product> getTopDeals(int limit) {
        List<Product> products = productRepository.findAll();

        // Use Max-Heap based on discount percentage
        PriorityQueue<Product> maxHeap = new PriorityQueue<>(
                (a, b) -> Double.compare(getDiscountPercent(b), getDiscountPercent(a))
        );

        for (Product p : products) {
            if (p.getDealType() != null && !"NONE".equalsIgnoreCase(p.getDealType())) {
                maxHeap.offer(p);
            }
        }

        List<Product> topDeals = new ArrayList<>();
        for (int i = 0; i < limit && !maxHeap.isEmpty(); i++) {
            topDeals.add(maxHeap.poll());
        }

        return topDeals;
    }

    /**
     * Helper — calculate discount percentage from price and sale price
     */
    private double getDiscountPercent(Product p) {
        try {
            double price = Double.parseDouble(p.getPrice().replace("$", "").trim());
            double sale = Double.parseDouble(p.getSalePrice().replace("$", "").trim());
            return ((price - sale) / price) * 100.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

}
