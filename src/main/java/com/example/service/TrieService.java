package com.example.service;

import com.example.model.TrieNode;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * TrieService implements autocomplete functionality using Trie data structure.
 * <p>
 * Trie (Prefix Tree) Algorithm:
 * - A tree where each node represents a character
 * - Paths from root to nodes form words
 * - Efficient for prefix-based searches: O(m) where m = prefix length
 * - Space complexity: O(ALPHABET_SIZE * N * M) where N = number of words, M = avg length
 * <p>
 * Operations:
 * 1. INSERT: Add word character by character, mark end node
 * 2. SEARCH: Traverse from root following prefix characters
 * 3. AUTOCOMPLETE: Find prefix node, then collect all words in its subtree
 * 4. RANKING: Sort suggestions by frequency (most popular first)
 */
@Service
public class TrieService {

    private final TrieNode root;
    private static final int MAX_SUGGESTIONS = 10;

    public TrieService() {
        this.root = new TrieNode();
    }

    /**
     * Insert a word into the Trie.
     * Algorithm:
     * 1. Start at root node
     * 2. For each character in word:
     * - If child node doesn't exist, create it
     * - Move to child node
     * 3. Mark last node as end of word
     * 4. Increment frequency counter
     * <p>
     * Time Complexity: O(m) where m = word length
     * Space Complexity: O(m) in worst case
     */
    public void insert(String word) {
        if (word == null || word.isEmpty()) return;

        word = word.toLowerCase().trim();
        TrieNode current = root;

        for (char ch : word.toCharArray()) {
            current.getChildren().putIfAbsent(ch, new TrieNode());
            current = current.getChildren().get(ch);
        }

        current.setEndOfWord(true);
        current.incrementFrequency();
    }

    /**
     * Search for exact word in Trie.
     * Algorithm:
     * 1. Traverse Trie following each character
     * 2. If any character path doesn't exist, return false
     * 3. Check if final node is marked as end of word
     * <p>
     * Time Complexity: O(m) where m = word length
     */
    public boolean search(String word) {
        if (word == null || word.isEmpty()) return false;

        word = word.toLowerCase().trim();
        TrieNode node = findNode(word);
        return node != null && node.isEndOfWord();
    }

    /**
     * Find the node corresponding to a prefix.
     * Returns null if prefix doesn't exist in Trie.
     */
    private TrieNode findNode(String prefix) {
        TrieNode current = root;

        for (char ch : prefix.toCharArray()) {
            TrieNode next = current.getChildren().get(ch);
            if (next == null) {
                return null;
            }
            current = next;
        }

        return current;
    }

    /**
     * Get autocomplete suggestions for a given prefix.
     * <p>
     * Algorithm:
     * 1. Find the node corresponding to the prefix (O(m))
     * 2. Perform DFS/BFS from that node to collect all words in subtree
     * 3. Store words with their frequencies
     * 4. Sort by frequency (descending) to get most popular terms first
     * 5. Return top N suggestions
     * <p>
     * Time Complexity: O(m + n*k) where:
     * - m = prefix length
     * - n = number of words with this prefix
     * - k = average word length
     * <p>
     * Space Complexity: O(n) for storing suggestions
     */
    public List<String> autocomplete(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return Collections.emptyList();
        }

        prefix = prefix.toLowerCase().trim();
        TrieNode prefixNode = findNode(prefix);

        if (prefixNode == null) {
            return Collections.emptyList();
        }

        List<SuggestionResult> suggestions = new ArrayList<>();
        collectWords(prefixNode, prefix, suggestions);

        // Sort by frequency (most popular first), then alphabetically
        suggestions.sort((a, b) -> {
            if (b.frequency != a.frequency) {
                return Integer.compare(b.frequency, a.frequency);
            }
            return a.word.compareTo(b.word);
        });

        // Return top N suggestions
        return suggestions.stream()
                .limit(MAX_SUGGESTIONS)
                .map(s -> s.word)
                .toList();
    }

    /**
     * Recursively collect all words in the subtree using DFS.
     * <p>
     * Algorithm (Depth-First Search):
     * 1. If current node is end of word, add to suggestions
     * 2. For each child character:
     * - Append character to current prefix
     * - Recursively collect words from child subtree
     * - Backtrack (remove character)
     * <p>
     * This explores all branches depth-first to find complete words.
     */
    private void collectWords(TrieNode node, String currentWord, List<SuggestionResult> suggestions) {
        if (node.isEndOfWord()) {
            suggestions.add(new SuggestionResult(currentWord, node.getFrequency()));
        }

        for (Map.Entry<Character, TrieNode> entry : node.getChildren().entrySet()) {
            collectWords(entry.getValue(), currentWord + entry.getKey(), suggestions);
        }
    }

    /**
     * Helper class to store word suggestions with their frequencies.
     */
    private static class SuggestionResult {
        String word;
        int frequency;

        SuggestionResult(String word, int frequency) {
            this.word = word;
            this.frequency = frequency;
        }
    }

    /**
     * Clear all data from the Trie.
     */
    public void clear() {
        root.getChildren().clear();
    }
}