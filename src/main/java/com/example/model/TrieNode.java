package com.example.model;

import java.util.HashMap;
import java.util.Map;

/**
 * TrieNode represents a single node in the Trie data structure.
 * Each node contains:
 * - A map of children nodes (character -> TrieNode)
 * - A flag indicating if this node marks the end of a word
 * - A frequency counter for ranking popular terms
 */
public class TrieNode {
    private Map<Character, TrieNode> children;
    private boolean isEndOfWord;
    private int frequency; // Track how often this word is searched

    public TrieNode() {
        this.children = new HashMap<>();
        this.isEndOfWord = false;
        this.frequency = 0;
    }

    public Map<Character, TrieNode> getChildren() {
        return children;
    }

    public boolean isEndOfWord() {
        return isEndOfWord;
    }

    public void setEndOfWord(boolean endOfWord) {
        isEndOfWord = endOfWord;
    }

    public int getFrequency() {
        return frequency;
    }

    public void incrementFrequency() {
        this.frequency++;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }
}