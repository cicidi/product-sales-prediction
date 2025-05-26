package com.example.productapi.service;

import com.example.productapi.model.Predications;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for handling product sales predictions within specific date ranges
 */
public interface PredictionService {

  /**
   * Predict sales for a product within a date range
   *
   * @param productId   ID of the product to predict sales for
   * @param sellerId    ID of the seller
   * @param priceToSale Discount price (if null or 0, original price will be used)
   * @param startDate   Start date for prediction (inclusive)
   * @param endDate     End date for prediction (exclusive), if null only startDate will be
   *                    predicted
   * @return Predications object containing aggregated prediction results
   */
  Predications predictSalesByProductId(String productId, String sellerId, Double priceToSale,
      LocalDate startDate, LocalDate endDate);

  List<Predications> predicateTopSales(String sellerId, String category, LocalDate startDate,
      LocalDate endDate, Integer topN);

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
} 