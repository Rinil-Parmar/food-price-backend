package com.example.controller;

import com.example.dto.CompareRequest;
import com.example.dto.RecommendRequest;
import com.example.model.ApiResponse;
import com.example.model.Product;
import com.example.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnalysisController {

    private final ProductService productService;

    //  Compare API
    @PostMapping("/compare")
    public ApiResponse<List<Product>> compareProducts(@RequestBody CompareRequest req) {
        return new ApiResponse<>("success", "Comparison complete", productService.compareProducts(req));
    }

    // Recommendation API
    @PostMapping("/recommend")
    public ApiResponse<List<String>> recommendStores(@RequestBody RecommendRequest req) {
        return new ApiResponse<>("success", "Recommendation generated", productService.recommendStores(req));
    }
}
