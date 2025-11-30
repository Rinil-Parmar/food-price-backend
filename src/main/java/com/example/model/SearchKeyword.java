package com.example.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "search_keywords")
@Data
public class SearchKeyword {
    @Id
    private String id;

    private String keyword;
    private long count;
}
