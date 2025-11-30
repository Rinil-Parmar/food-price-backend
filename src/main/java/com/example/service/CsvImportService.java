package com.example.service;

import com.example.model.Product;
import com.example.repository.ProductRepository;
import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CsvImportService {

    private final ProductRepository productRepository;
    private final ProductService productService; // Inject ProductService

    /**
     * Imports products from the local CSV file into MongoDB.
     * Clears previous data before import.
     *
     * @return the number of products imported
     * @throws RuntimeException if CSV cannot be read or import fails
     */
    public int importCsv() {
        int count = 0;

        try (CSVReader reader = new CSVReader(
                new InputStreamReader(new ClassPathResource("products1.csv").getInputStream()))) {

            // Step 1: Clear previous data
            productRepository.deleteAll();

            // Step 2: Skip header row
            reader.readNext();

            String[] nextLine;
            List<Product> products = new ArrayList<>();

            // Step 3: Read CSV rows
            while ((nextLine = reader.readNext()) != null) {
                Product product = new Product();
                product.setProductName(nextLine[0]);
                product.setPrice(nextLine[1]);  // store price as string
                product.setDescription(nextLine[2]);
                product.setImageUrl(nextLine[3]);
                product.setAvailability(nextLine[4]);
                product.setCategory(nextLine[5]);
                product.setStoreName(nextLine[6]);
                product.setProductUrl(nextLine[7]);
                products.add(product);
                count++;
            }

            // Step 4: Save all products to MongoDB
            productRepository.saveAll(products);
            // Reload cache for fast access
            productService.reloadCache();

        } catch (Exception e) {
            // Rethrow exception to controller for proper ApiResponse handling
            throw new RuntimeException("Error importing CSV: " + e.getMessage(), e);
        }

        return count;
    }
}
