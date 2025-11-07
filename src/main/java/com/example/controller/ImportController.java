package com.example.controller;


import com.example.service.CsvImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final CsvImportService csvImportService;

    @PostMapping
    public String importCsv() {
        csvImportService.importCsv();
        return "Import started from local CSV file.";
    }

}

