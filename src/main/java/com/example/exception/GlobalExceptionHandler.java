package com.example.exception;

import com.example.model.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

/**
 * Global Exception Handler for the application.
 * Catches all unhandled exceptions and returns a unified JSON response.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    //  Handle invalid arguments (validation errors, empty fields, etc.)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        ApiResponse<String> response = new ApiResponse<>(
                "error",
                ex.getMessage(),
                null
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    //  Handle @Valid validation errors (if you use @Valid later)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<String>> handleValidationErrors(MethodArgumentNotValidException ex) {

        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ApiResponse<String> response = new ApiResponse<>(
                "error",
                errors,
                null
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // ❗ Handle all other unhandled exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGeneralException(Exception ex) {

        ApiResponse<String> response = new ApiResponse<>(
                "error",
                "An unexpected error occurred: " + ex.getMessage(),
                null
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
