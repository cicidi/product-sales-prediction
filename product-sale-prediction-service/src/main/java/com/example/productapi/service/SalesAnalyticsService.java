package com.example.productapi.service;

import com.example.productapi.dto.TopSellingProductResponse;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for handling sales analytics operations
 */
public interface SalesAnalyticsService {

    /**
     * Get top selling products based on search criteria
     *
     * @param sellerId Seller ID to filter by (optional)
     * @param category Optional product category to filter by
     * @param startTime Start time for analytics
     * @param endTime End time for analytics
     * @param topN Number of top products to return
     * @param includeRevenue Whether to include revenue information in the results
     * @return List of top selling products with their details and metrics
     */
    List<TopSellingProductResponse> getTopSellingProducts(
            String sellerId,
            String category,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int topN,
            boolean includeRevenue);
}