package com.example.dto;

import lombok.Data;

import java.util.List;

@Data
public class CompareRequest {
    private String category;
    private List<Double> priceRange;
    private List<String> stores;
    private boolean availability;
}
