package com.example.dto;

import lombok.Data;

import java.util.List;

@Data
public class CompareRequest {
    private String category;
    private List<Double> priceRange;   // [min, max]
    private List<String> stores;       // store names
    private boolean availability;      // in-stock only
    private boolean saleOnly;          // NEW → filter only sale items
}
