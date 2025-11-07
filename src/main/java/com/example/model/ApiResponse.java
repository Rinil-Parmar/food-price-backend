package com.example.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private String status;   // "success" or "error"
    private String message;  // descriptive message
    private T data;          // actual response data (generic type)
}
