package com.example.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;

@Data
@Document(collection = "products")
public class Product {

    @Id
    private String id;
    private String productName;
    //    private double price;
    private String price;

    private String description;
    private String imageUrl;
    private String availability;
    private String category;
    private String storeName;
    private String productUrl;
}
