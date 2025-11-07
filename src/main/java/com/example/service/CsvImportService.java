package com.example.service;


import com.example.model.Product;
import com.example.repository.ProductRepository;
import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CsvImportService {

    private final ProductRepository productRepository;

    public void importCsv() {
        try (CSVReader reader = new CSVReader(
                new InputStreamReader(new ClassPathResource("products.csv").getInputStream()))) {

            // Step 1: Clear old data
            productRepository.deleteAll();
            System.out.println("Old data cleared from MongoDB.");

            // skip header
            reader.readNext();
            String[] nextLine;
            List<Product> products = new ArrayList<>();

            while ((nextLine = reader.readNext()) != null) {
                Product product = new Product();
                product.setProductName(nextLine[0]);
//                String priceText = nextLine[1].replace("$", "").replace("/kg", "").trim();
//                double price = 0.0;
//                try {
//                    price = Double.parseDouble(priceText);
//                } catch (NumberFormatException e) {
//                    System.err.println("⚠️ Skipping invalid price: " + nextLine[1]);
//                }
//                product.setPrice(price);
                product.setPrice(nextLine[1]);


                product.setDescription(nextLine[2]);
                product.setImageUrl(nextLine[3]);
                product.setAvailability(nextLine[4]);
                product.setCategory(nextLine[5]);
                product.setStoreName(nextLine[6]);
                product.setProductUrl(nextLine[7]);
                products.add(product);
            }

            productRepository.saveAll(products);
            System.out.println("✅ Imported " + products.size() + " products from local CSV.");
        } catch (Exception e) {
            System.err.println("❌ Error importing CSV: " + e.getMessage());
        }
    }
}
