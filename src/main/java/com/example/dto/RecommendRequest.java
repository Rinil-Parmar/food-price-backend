package com.example.dto;

import lombok.Data;

import java.util.List;

/**
 * Request model for store recommendation.
 * <p>
 * Fields:
 * - preferredProvider: user’s preferred store (optional)
 * - productNeeds: list of products user wants to buy
 * - budget: user’s spending level (e.g., low, medium, high)
 * - delivery: delivery or pickup preference
 * - loyaltyProgram: true if user has loyalty membership
 */
@Data
public class RecommendRequest {
    private String preferredProvider;
    private List<String> productNeeds;
    private String budget;
    private String delivery;
    private boolean loyaltyProgram;
}
