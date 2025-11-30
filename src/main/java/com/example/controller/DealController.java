package com.example.controller;

import com.example.model.ApiResponse;
import com.example.model.Product;
import com.example.service.DealService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deals")
@RequiredArgsConstructor
public class DealController {

    private final DealService dealService;

    /**
     * POST /api/deals/add
     * Randomly assign salePrice, loyaltyPrice, and dealType
     * to existing products in MongoDB.
     */
    @PostMapping("/add")
    public ApiResponse<String> assignRandomDeals() {
        String result = dealService.assignRandomDeals();
        return new ApiResponse<>("success", result, null);
    }

    @GetMapping("/top")
    public ApiResponse<List<Product>> getTopDeals(@RequestParam(defaultValue = "10") int limit) {
        List<Product> deals = dealService.getTopDeals(limit);
        return new ApiResponse<>("success", "Top " + limit + " deals fetched.", deals);
    }
}
