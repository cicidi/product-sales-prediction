package com.example.productapi.service;

import java.util.Map;

/**
 * Product Similarity Search Service
 */
public interface ProductSimilarityService {
    
    /**
     * Find similar products
     * 
     * @param request Contains search parameters:
     *                - description: Product description text, used to find products similar to this description
     *                - productId: Product ID, used to find other products similar to this product
     *                - limit: Limit on the number of results returned
     * @return Map containing results:
     *         - products: List of similar products
     *         - count: Number of products returned
     */
    Map<String, Object> findSimilarProducts(Map<String, Object> request);
} 