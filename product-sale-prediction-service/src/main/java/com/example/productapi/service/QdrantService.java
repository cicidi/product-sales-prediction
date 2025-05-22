package com.example.productapi.service;

import java.util.List;
import java.util.Map;

/**
 * Service for interacting with Qdrant vector database
 */
public interface QdrantService {
    
    /**
     * Upsert a point into Qdrant
     */
    void upsertPoint(String pointId, List<Float> vector, Map<String, Object> payload);
    
    /**
     * Delete a point from Qdrant
     */
    void deletePoint(String pointId);
    
    /**
     * Find nearest points to a query vector
     */
    List<Map<String, Object>> findNearest(List<Float> vector, int limit);
} 