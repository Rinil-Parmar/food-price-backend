package com.example.repository;

import com.example.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByCategory(String category);

    boolean existsByProductNameIgnoreCase(String productName);

}