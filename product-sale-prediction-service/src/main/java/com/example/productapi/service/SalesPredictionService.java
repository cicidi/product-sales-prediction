package com.example.productapi.service;

import com.example.productapi.model.Product;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Service for handling product sales predictions
 */
public interface SalesPredictionService {
    /**
     * Predict sales for a product using historical data
     * 
     * @param product Product to predict sales for
     * @param sellerId ID of the seller
     * @param startTime Start time for historical data and prediction
     * @param endTime End time for prediction
     * @return Map containing prediction data
     */
    Map<String, Object> predictProductSales(Product product, String sellerId, 
                                           LocalDateTime startTime, LocalDateTime endTime);
} 