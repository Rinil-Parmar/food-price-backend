package com.example.service;

import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchTrackingService {


    //  Search Tracking Using KMP + HashMap
// -------------------------------------------------------------------------
// Stores keyword -> search frequency (in-memory)
    private final Map<String, Integer> keywordFrequency = new HashMap<>();

    /**
     * Record a search keyword using HashMap as frequency store.
     * Implements requirement of KMP-based search tracking.
     */
    public void recordSearch(String keyword) {
        if (keyword == null || keyword.isBlank()) return;
        String normalized = keyword.toLowerCase().trim();

        keywordFrequency.put(
                normalized,
                keywordFrequency.getOrDefault(normalized, 0) + 1
        );
    }

    /**
     * Get search frequency for a keyword.
     */
    public int getKeywordFrequency(String keyword) {
        if (keyword == null || keyword.isBlank()) return 0;
        return keywordFrequency.getOrDefault(keyword.toLowerCase().trim(), 0);
    }

    /**
     * Returns Top N searched keywords ordered by frequency.
     */
    public List<Map.Entry<String, Integer>> getTopKeywords(int limit) {
        return keywordFrequency.entrySet()
                .stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(limit)
                .collect(Collectors.toList());
    }


// KMP Algorithm (Knuth–Morris–Pratt)
// Used here to demonstrate searching logic as per assignment requirement.
// -------------------------------------------------------------------------

    /**
     * KMP Search: Counts occurrences of pattern inside given text.
     */
    public int kmpSearch(String text, String pattern) {
        if (pattern.isEmpty()) return 0;

        int[] lps = buildLPS(pattern);
        int i = 0, j = 0, count = 0;

        while (i < text.length()) {
            if (text.charAt(i) == pattern.charAt(j)) {
                i++;
                j++;
                if (j == pattern.length()) {
                    count++;
                    j = lps[j - 1];  // shift using LPS
                }
            } else {
                if (j != 0) j = lps[j - 1];
                else i++;
            }
        }
        return count;
    }

    /**
     * Build LPS (Longest Prefix Suffix) array for KMP.
     */
    private int[] buildLPS(String pattern) {
        int[] lps = new int[pattern.length()];
        int len = 0, i = 1;

        while (i < pattern.length()) {
            if (pattern.charAt(i) == pattern.charAt(len)) {
                lps[i] = ++len;
                i++;
            } else {
                if (len != 0) len = lps[len - 1];
                else i++;
            }
        }
        return lps;
    }


// -------------------------------------------------------------------------
// Count occurrences in files using Boyer–Moore
// -------------------------------------------------------------------------

    /**
     * Boyer–Moore string search counting logic.
     */
    public int boyerMooreCount(String text, String pattern) {
        if (pattern.isEmpty()) return 0;

        int[] last = buildLastOccurrence(pattern);
        int count = 0;
        int i = pattern.length() - 1;

        while (i < text.length()) {
            int j = pattern.length() - 1;

            while (j >= 0 && pattern.charAt(j) == text.charAt(i - (pattern.length() - 1 - j))) {
                j--;
            }

            if (j < 0) {
                count++;
                i += pattern.length(); // move ahead after match
            } else {
                i += Math.max(1, j - last[text.charAt(i)]);
            }
        }
        return count;
    }

    /**
     * Builds last occurrence table for Boyer–Moore algorithm.
     */
    private int[] buildLastOccurrence(String pattern) {
        int[] last = new int[256];
        Arrays.fill(last, -1);

        for (int i = 0; i < pattern.length(); i++) {
            last[pattern.charAt(i)] = i;
        }
        return last;
    }

    /**
     * Count occurrences of a keyword in a text file using Boyer–Moore.
     */
    public int countWordInFile(String keyword, String filePath) {
        try {
            String content = Files.readString(Path.of(filePath)).toLowerCase();
            return boyerMooreCount(content, keyword.toLowerCase());
        } catch (Exception e) {
            return 0;
        }
    }


}
