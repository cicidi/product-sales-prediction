package com.example.productapi.service.impl;

import com.example.productapi.model.Order;
import com.example.productapi.model.Product;
import com.example.productapi.model.SalesPredictionRequest;
import com.example.productapi.repository.OrderRepository;
import com.example.productapi.repository.ProductRepository;
import com.example.productapi.service.EmbeddingService;
import com.example.productapi.service.MLModelService;
import com.example.productapi.service.OrderService;
import com.example.productapi.service.SalesPredictionService;
import com.example.productapi.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SalesPredictionServiceImpl implements SalesPredictionService {

  private static final Logger logger = LoggerFactory.getLogger(SalesPredictionServiceImpl.class);

  private final OrderRepository orderRepository;
  private final ProductRepository productRepository;
  private final OrderService orderService;
  private final EmbeddingService embeddingService;
  private final MLModelService mlModelService;

  @Autowired
  public SalesPredictionServiceImpl(OrderRepository orderRepository,
      ProductRepository productRepository,
      OrderService orderService,
      EmbeddingService embeddingService,
      MLModelService mlModelService) {
    this.orderRepository = orderRepository;
    this.productRepository = productRepository;
    this.orderService = orderService;
    this.embeddingService = embeddingService;
    this.mlModelService = mlModelService;
  }

  private List<Order> getHistoricalOrders(String productId, String sellerId) {
    // 获取过去6个月的销售数据
    LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
    return orderRepository.findBySellerIdAndProductIdAndTimestampAfter(
        sellerId, productId, sixMonthsAgo);
  }

  @Override
  public Map<String, Object> predictProductSales(Product product, String sellerId,
      LocalDateTime startTime, LocalDateTime endTime) {
    // Use the ML model for prediction
    Double unitPrice = product.getPrice();
    Integer weeksAhead = 12; // Default to predict 12 weeks (approximately 3 months)

    // Get ML prediction
    Map<String, Object> mlPrediction = mlModelService.predictFutureSales(
        product.getId(), sellerId, unitPrice, weeksAhead);

    // Extract weekly predictions
    List<Map<String, Object>> weeklyPredictions = mlModelService.getWeeklyPredictions(mlPrediction);

    // Create prediction data
    Map<String, Object> prediction = new HashMap<>();

    // Calculate total predicted sales quantity
    double totalPredictedSales = weeklyPredictions.stream()
        .mapToDouble(week -> ((Number) week.get("predicted_sales")).doubleValue())
        .sum();

    prediction.put("predictedSalesQuantity", (int) Math.round(totalPredictedSales));
    prediction.put("confidenceScore", 0.85); // Fixed for now
    prediction.put("trendDirection", determineTrend(weeklyPredictions));

    // Get historical data for context
    List<Order> historicalOrders = getHistoricalOrders(product.getId(), sellerId);
    int totalHistoricalSales = historicalOrders.stream()
        .mapToInt(Order::getQuantity)
        .sum();
    prediction.put("historicalSalesCount", totalHistoricalSales);

    // Add monthly forecast (group the weekly predictions by month)
    List<Map<String, Object>> monthlyForecast = groupPredictionsByMonth(weeklyPredictions);
    prediction.put("monthlyForecast", monthlyForecast);

    // Add raw ML prediction data for reference
    prediction.put("mlPredictionData", mlPrediction);

    return prediction;
  }

  private String determineTrend(List<Map<String, Object>> weeklyPredictions) {
    if (weeklyPredictions.size() < 2) {
      return "steady";
    }

    double firstWeek = ((Number) weeklyPredictions.get(0).get("predicted_sales")).doubleValue();
    double lastWeek = ((Number) weeklyPredictions.get(weeklyPredictions.size() - 1)
        .get("predicted_sales")).doubleValue();

    double change = (lastWeek - firstWeek) / firstWeek;

    if (change > 0.05) {
      return "up";
    } else if (change < -0.05) {
      return "down";
    } else {
      return "steady";
    }
  }

  private List<Map<String, Object>> groupPredictionsByMonth(
      List<Map<String, Object>> weeklyPredictions) {
    // Group by month
    Map<String, Double> monthlyTotals = new HashMap<>();
    Map<String, LocalDateTime> monthDates = new HashMap<>();

    DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern("yyyy-MM");

    for (Map<String, Object> weekPrediction : weeklyPredictions) {
      String predictionDate = (String) weekPrediction.get("prediction_date");
      LocalDateTime date = LocalDateTime.parse(predictionDate + "T00:00:00",
          DateTimeFormatter.ISO_LOCAL_DATE_TIME);
      String monthKey = date.format(monthFormat);
      double sales = ((Number) weekPrediction.get("predicted_sales")).doubleValue();

      monthlyTotals.merge(monthKey, sales, Double::sum);
      if (!monthDates.containsKey(monthKey)) {
        monthDates.put(monthKey, date);
      }
    }

    // Convert to list of maps
    List<Map<String, Object>> monthlyForecast = new ArrayList<>();
    for (Map.Entry<String, Double> entry : monthlyTotals.entrySet()) {
      Map<String, Object> month = new HashMap<>();
      month.put("month", entry.getKey());
      month.put("predictedQuantity", (int) Math.round(entry.getValue()));
      monthlyForecast.add(month);
    }

    // Sort by month
    monthlyForecast.sort(Comparator.comparing(m -> (String) m.get("month")));

    return monthlyForecast;
  }
}