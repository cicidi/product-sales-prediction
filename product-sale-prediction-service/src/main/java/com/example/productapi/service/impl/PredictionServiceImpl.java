package com.example.productapi.service.impl;

import com.example.productapi.dto.PythonPredictionRequest;
import com.example.productapi.model.Order;
import com.example.productapi.model.Predication;
import com.example.productapi.model.Predications;
import com.example.productapi.model.Product;
import com.example.productapi.repository.OrderRepository;
import com.example.productapi.repository.ProductRepository;
import com.example.productapi.service.PredictionService;
import com.example.productapi.service.PythonPredictionClient;
import com.example.productapi.util.HolidayChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.services.sagemakerruntime.endpoints.internal.Value.Int;
import java.util.stream.Collectors;

@Service
public class PredictionServiceImpl implements PredictionService {

  private static final Logger logger = LoggerFactory.getLogger(PredictionServiceImpl.class);

  private final ProductRepository productRepository;
  private final OrderRepository orderRepository;
  private final PythonPredictionClient pythonPredictionClient;

  @Autowired
  public PredictionServiceImpl(ProductRepository productRepository, OrderRepository orderRepository,
      PythonPredictionClient pythonPredictionClient) {
    this.productRepository = productRepository;
    this.orderRepository = orderRepository;
    this.pythonPredictionClient = pythonPredictionClient;
  }

  @PostConstruct
  public void initializeModel() {
    // This method is now empty as the model initialization logic has been moved to the PythonPredictionClient
  }

  @Override
  public List<Predications> predicateTopSales(String sellerId, String category, LocalDate startDate,
      LocalDate endDate, Integer topN) {
    List<Product> products = productRepository.findBySellerIdAndCategory(sellerId, category);
    List<Predications> predicationsList = new ArrayList<>();
    for (Product product : products) {
      Predications predications = predictSalesByProductId(product.getId(), sellerId,
          product.getPrice(), startDate, endDate);
      predicationsList.add(predications);
    }
    return predicationsList.stream()
        .sorted((a, b) -> Integer.compare(b.getTotalQuantity(), a.getTotalQuantity()))
        .limit(topN)
        .collect(Collectors.toList());
  }

  @Override
  public Predications predictSalesByProductId(String productId, String sellerId, Double priceToSale,
      LocalDate startDate, LocalDate endDate) {

    // Get product details
    Product product = productRepository.findById(productId)
        .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

    // If endDate is null, only predict for startDate
    if (endDate == null) {
      endDate = startDate;
    }

    // Calculate total days
    long totalDays = ChronoUnit.DAYS.between(startDate, endDate.plusDays(1));

    // Get historical sales data for lag features
    List<Order> historicalOrders = getHistoricalOrders(productId, sellerId, startDate);

    // Generate daily predictions
    List<Predication> predictions = new ArrayList<>();
    int totalQuantity = 0;

    // Optimize: use batch prediction for multiple days, single prediction for one day
    if (totalDays == 1) {
      // Single day prediction
      Map<String, Object> features = prepareFeatures(productId, sellerId, priceToSale,
          startDate, historicalOrders);
      int quantity = predictDailySales(features);

      predictions.add(Predication.builder()
          .date(startDate)
          .quantity(quantity)
          .build());

      totalQuantity = quantity;

    } else {
      // Multiple days - use batch prediction for better performance
      List<Map<String, Object>> allFeatures = new ArrayList<>();
      List<LocalDate> dates = new ArrayList<>();

      // Prepare features for all dates
      for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
        Map<String, Object> features = prepareFeatures(productId, sellerId, priceToSale,
            date, historicalOrders);
        allFeatures.add(features);
        dates.add(date);
      }

      // Try batch prediction first
      List<Integer> quantities = predictBatchDailySales(allFeatures);

      // If batch prediction failed, fall back to individual predictions
      if (quantities == null || quantities.size() != dates.size()) {
        logger.warn("Batch prediction failed, falling back to individual predictions");
        quantities = new ArrayList<>();
        for (Map<String, Object> features : allFeatures) {
          quantities.add(predictDailySales(features));
        }
      }

      // Build predictions list
      for (int i = 0; i < dates.size(); i++) {
        int quantity = quantities.get(i);
        predictions.add(Predication.builder()
            .date(dates.get(i))
            .quantity(quantity)
            .build());
        totalQuantity += quantity;
      }
    }

    // Build and return Predications object
    return Predications.builder()
        .productId(productId)
        .predicationList(predictions)
        .startDate(startDate)
        .endDate(endDate)
        .totalQuantity(totalQuantity)
        .totalDays((int) totalDays)
        .build();
  }

  /**
   * Prepare features for prediction using the specific features required by the Python prediction
   * service Features: product_id, seller_id, sale_price, original_price, is_holiday, is_weekend,
   * day_of_week, day_of_month, month, lag_1, lag_7, lag_30
   */
  private Map<String, Object> prepareFeatures(String productId, String sellerId, Double priceToSale,
      LocalDate predictionDate,
      List<Order> historicalOrders) {

    // Retrieve product details
    Product product = productRepository.findById(productId)
        .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

    // Determine price to use - if priceToSale is null or 0, use original price (no discount)
    Double salePrice = (priceToSale == null || priceToSale == 0) ? product.getPrice() : priceToSale;
    Double originalPrice = product.getPrice();

    // Prepare features map with ALL features needed by Python API
    Map<String, Object> features = new HashMap<>();

    // Product and seller identifiers (now included for Python API)
    features.put("product_id", productId);
    features.put("seller_id", sellerId);

    // Price features
    features.put("sale_price", salePrice);
    features.put("original_price", originalPrice);

    // Date-based features
    features.put("is_holiday", HolidayChecker.isHoliday(predictionDate) ? 1 : 0);
    features.put("is_weekend", HolidayChecker.isWeekend(predictionDate) ? 1 : 0);
    features.put("day_of_week",
        predictionDate.getDayOfWeek().getValue() - 1); // Python weekday: Monday=0, Sunday=6
    features.put("day_of_month", predictionDate.getDayOfMonth());
    features.put("month", predictionDate.getMonthValue());

    // Lag features - get historical sales quantities
    Map<String, Double> lagFeatures = calculateLagFeatures(historicalOrders, predictionDate);
    features.put("lag_1", lagFeatures.get("lag_1"));
    features.put("lag_7", lagFeatures.get("lag_7"));
    features.put("lag_30", lagFeatures.get("lag_30"));

    return features;
  }

  /**
   * Calculate lag features (lag_1, lag_7, lag_30) based on historical orders
   */
  private Map<String, Double> calculateLagFeatures(List<Order> historicalOrders,
      LocalDate predictionDate) {
    Map<String, Double> lagFeatures = new HashMap<>();

    // Group orders by date and sum quantities
    Map<LocalDate, Integer> dailySales = new HashMap<>();
    for (Order order : historicalOrders) {
      LocalDate orderDate = order.getTimestamp().toLocalDate();
      dailySales.merge(orderDate, order.getQuantity(), Integer::sum);
    }

    // Calculate lag features
    lagFeatures.put("lag_1", getDailySales(dailySales, predictionDate.minusDays(1)));
    lagFeatures.put("lag_7", getDailySales(dailySales, predictionDate.minusDays(7)));
    lagFeatures.put("lag_30", getDailySales(dailySales, predictionDate.minusDays(30)));

    return lagFeatures;
  }

  /**
   * Get daily sales for a specific date, return 0.0 if no sales
   */
  private Double getDailySales(Map<LocalDate, Integer> dailySales, LocalDate date) {
    return dailySales.getOrDefault(date, 0).doubleValue();
  }

  /**
   * Get historical orders for lag feature calculation
   */
  private List<Order> getHistoricalOrders(String productId, String sellerId, LocalDate startDate) {
    // Get orders from last 60 days to have enough data for lag features
    LocalDateTime cutoffDate = startDate.minusDays(60).atStartOfDay();
    return orderRepository.findBySellerIdAndProductIdAndTimestampAfter(sellerId, productId,
        cutoffDate);
  }

  /**
   * Predict sales for a specific day using the Python prediction service
   */
  private int predictDailySales(Map<String, Object> features) {

    // Check if Python service is available
    if (!pythonPredictionClient.isServiceAvailable()) {
      logger.warn("Python prediction service is not available, using fallback logic");
      return predictWithFallbackLogic(features);
    }

    try {
      // Build Python prediction request from features
      PythonPredictionRequest request = PythonPredictionRequest.builder()
          .productId((String) features.get("product_id"))
          .sellerId((String) features.get("seller_id"))
          .salePrice((Double) features.get("sale_price"))
          .originalPrice((Double) features.get("original_price"))
          .isHoliday((Integer) features.get("is_holiday"))
          .isWeekend((Integer) features.get("is_weekend"))
          .dayOfWeek((Integer) features.get("day_of_week"))
          .dayOfMonth((Integer) features.get("day_of_month"))
          .month((Integer) features.get("month"))
          .lag1((Double) features.get("lag_1"))
          .lag7((Double) features.get("lag_7"))
          .lag30((Double) features.get("lag_30"))
          .build();

      logger.debug("Calling Python prediction service with request: {}", request);

      // Call Python service
      Double prediction = pythonPredictionClient.predictSingle(request);

      if (prediction != null) {
        int result = Math.max(0, (int) Math.round(prediction));
        logger.debug("Python service returned prediction: {}", result);
        return result;
      } else {
        logger.warn("Python service returned null, using fallback logic");
        return predictWithFallbackLogic(features);
      }

    } catch (Exception e) {
      logger.error("Error calling Python prediction service: {}", e.getMessage(), e);
      logger.warn("Falling back to simple prediction logic");
      return predictWithFallbackLogic(features);
    }
  }

  /**
   * Simple fallback prediction logic when Python service is not available
   */
  private int predictWithFallbackLogic(Map<String, Object> features) {
    logger.debug("Using fallback prediction logic with features: {}", features);

    // Simple heuristic-based prediction
    double salePrice = (Double) features.getOrDefault("sale_price", 100.0);
    double originalPrice = (Double) features.getOrDefault("original_price", 100.0);
    int isWeekend = (Integer) features.getOrDefault("is_weekend", 0);
    int isHoliday = (Integer) features.getOrDefault("is_holiday", 0);
    double lag1 = (Double) features.getOrDefault("lag_1", 0.0);
    double lag7 = (Double) features.getOrDefault("lag_7", 0.0);

    // Simple prediction logic based on business rules
    double basePrediction = 5.0; // Base sales quantity

    // Price discount effect
    double discountFactor = originalPrice > 0 ? (originalPrice - salePrice) / originalPrice : 0;
    if (discountFactor > 0) {
      basePrediction += discountFactor * 10; // More discount = more sales
    }

    // Weekend effect
    if (isWeekend == 1) {
      basePrediction *= 1.3; // 30% more sales on weekends
    }

    // Holiday effect
    if (isHoliday == 1) {
      basePrediction *= 1.5; // 50% more sales on holidays
    }

    // Historical sales effect
    double avgLag = (lag1 + lag7) / 2.0;
    if (avgLag > 0) {
      basePrediction = basePrediction * 0.7 + avgLag * 0.3; // Mix with historical data
    }

    int prediction = Math.max(1, (int) Math.round(basePrediction));
    logger.debug("Fallback prediction result: {}", prediction);
    return prediction;
  }

  /**
   * Predict sales for multiple days using batch prediction for better performance
   */
  private List<Integer> predictBatchDailySales(List<Map<String, Object>> allFeatures) {

    // Check if Python service is available
    if (!pythonPredictionClient.isServiceAvailable()) {
      logger.warn("Python prediction service is not available, using fallback logic for batch");
      return allFeatures.stream()
          .map(this::predictWithFallbackLogic)
          .collect(java.util.stream.Collectors.toList());
    }

    try {
      // Build batch request from all features
      List<PythonPredictionRequest> requests = allFeatures.stream()
          .map(features -> PythonPredictionRequest.builder()
              .productId((String) features.get("product_id"))
              .sellerId((String) features.get("seller_id"))
              .salePrice((Double) features.get("sale_price"))
              .originalPrice((Double) features.get("original_price"))
              .isHoliday((Integer) features.get("is_holiday"))
              .isWeekend((Integer) features.get("is_weekend"))
              .dayOfWeek((Integer) features.get("day_of_week"))
              .dayOfMonth((Integer) features.get("day_of_month"))
              .month((Integer) features.get("month"))
              .lag1((Double) features.get("lag_1"))
              .lag7((Double) features.get("lag_7"))
              .lag30((Double) features.get("lag_30"))
              .build())
          .collect(java.util.stream.Collectors.toList());

      logger.debug("Calling Python batch prediction service with {} requests", requests.size());

      // Call Python batch service
      List<Double> predictions = pythonPredictionClient.predictBatch(requests);

      if (predictions != null && predictions.size() == requests.size()) {
        List<Integer> results = predictions.stream()
            .map(prediction -> Math.max(0, (int) Math.round(prediction)))
            .collect(java.util.stream.Collectors.toList());
        logger.debug("Python batch service returned {} predictions", results.size());
        return results;
      } else {
        logger.warn("Python batch service returned invalid response");
        return null;
      }

    } catch (Exception e) {
      logger.error("Error calling Python batch prediction service: {}", e.getMessage(), e);
      return null;
    }
  }

  @Override
  public boolean isModelInitialized() {
    return pythonPredictionClient.isServiceAvailable();
  }
} 