package com.example.controller;

import com.example.model.ApiResponse;
import com.example.service.CsvImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final CsvImportService csvImportService;

    @PostMapping
    public ApiResponse<String> importCsv() {
        try {
            int importedCount = csvImportService.importCsv();  // update service to return count
            return new ApiResponse<>(
                    "success",
                    "CSV imported successfully",
                    importedCount + " products imported"
            );
        } catch (Exception e) {
            return new ApiResponse<>(
                    "error",
                    "Failed to import CSV: " + e.getMessage(),
                    null
            );
        }
    }
}
