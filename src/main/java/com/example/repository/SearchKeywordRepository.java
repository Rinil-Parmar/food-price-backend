package com.example.repository;

import com.example.model.SearchKeyword;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

public interface SearchKeywordRepository extends MongoRepository<SearchKeyword, String> {
    Optional<SearchKeyword> findByKeyword(String keyword);

    // Sort by count desc and limit results
    @Query(value = "{}", sort = "{ count: -1 }")
    List<SearchKeyword> findTopSearches(Pageable pageable);
}
