package com.example.productapi.service;

import java.util.List;

/**
 * Service for generating and managing embeddings
 */
public interface EmbeddingService {

    /**
     * Generate embedding for given text
     */
    List<Float> generateEmbedding(String text);
    
    /**
     * Store embedding for a product with its text description
     */
    void storeEmbedding(String productId, String text);
    
    /**
     * Get stored embedding for a product
     */
    List<Float> getEmbedding(String productId);
    
    /**
     * Delete embedding for a product
     */
    void deleteEmbedding(String productId);
    
    /**
     * Find similar products based on text description
     */
    List<String> findSimilarProducts(String text, int limit);
}