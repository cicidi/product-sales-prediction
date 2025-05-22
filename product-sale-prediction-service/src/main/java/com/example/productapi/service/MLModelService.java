package com.example.productapi.service;

import java.util.List;
import java.util.Map;

/**
 * Service for handling machine learning model operations
 */
public interface MLModelService {
    
    /**
     * Initialize the model by loading necessary resources
     */
    void initializeModel();
    
    /**
     * Check if the model is initialized
     * 
     * @return true if the model is initialized, false otherwise
     */
    boolean isModelInitialized();
    
    /**
     * Predict future sales for a specific product and seller
     * 
     * @param productId The ID of the product
     * @param sellerId The ID of the seller
     * @param unitPrice The unit price of the product
     * @param weeksAhead The number of weeks to predict into the future
     * @return A map containing the prediction result
     */
    Map<String, Object> predictFutureSales(String productId, String sellerId, Double unitPrice, Integer weeksAhead);
    
    /**
     * Get a list of predictions for multiple weeks
     * 
     * @param prediction The prediction result from predictFutureSales
     * @return A list of week-by-week predictions with sales quantities
     */
    List<Map<String, Object>> getWeeklyPredictions(Map<String, Object> prediction);
} 