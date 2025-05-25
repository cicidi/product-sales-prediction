package com.example.productapi.service.impl;

import com.example.productapi.dto.GetOrdersResponse;
import com.example.productapi.dto.ProductSalesSummary;
import com.example.productapi.model.Order;
import com.example.productapi.repository.OrderRepository;
import com.example.productapi.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

  private final OrderRepository orderRepository;

  @Autowired
  public OrderServiceImpl(OrderRepository orderRepository) {
    this.orderRepository = orderRepository;
  }

  /**
   * Get orders by seller ID with pagination
   */
  @Override
  public Page<Order> getOrdersBySeller(String sellerId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    return orderRepository.findBySellerIdOrderByTimestampDesc(sellerId, pageable);
  }

  /**
   * Get orders with filters
   */
  @Override
  public Page<Order> getOrdersWithFilters(String sellerId, String productId, String category,
      LocalDateTime startTime, LocalDateTime endTime, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    return orderRepository.findOrdersWithFilters(sellerId, productId, category, startTime, endTime,
        pageable);
  }

  @Override
  public Map<String, Object> generateAggregationData(List<Order> orders) {
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    Map<String, Map<String, Integer>> dailyProductQuantity = new HashMap<>();
    
    // Aggregate quantity by productId and date
    for (Order order : orders) {
      String date = order.getTimestamp().format(dateFormatter);
      String orderProductId = order.getProductId();
      
      dailyProductQuantity
          .computeIfAbsent(date, k -> new HashMap<>())
          .merge(orderProductId, order.getQuantity(), Integer::sum);
    }
    
    // Convert to dailyProductSales list format
    List<Map<String, Object>> dailyProductSales = new ArrayList<>();
    for (Map.Entry<String, Map<String, Integer>> dateEntry : dailyProductQuantity.entrySet()) {
      String date = dateEntry.getKey();
      for (Map.Entry<String, Integer> productEntry : dateEntry.getValue().entrySet()) {
        Map<String, Object> salesData = new HashMap<>();
        salesData.put("productId", productEntry.getKey());
        salesData.put("quantity", productEntry.getValue());
        salesData.put("date", date);
        dailyProductSales.add(salesData);
      }
    }
    
    // Sort dailyProductSales by date descending
    dailyProductSales.sort((a, b) -> ((String) b.get("date")).compareTo((String) a.get("date")));
    
    // Calculate totalSummary
    Map<String, Integer> productTotals = new HashMap<>();
    for (Order order : orders) {
      productTotals.merge(order.getProductId(), order.getQuantity(), Integer::sum);
    }
    
    // Convert totalSummary to list format
    List<Map<String, Object>> totalSummary = productTotals.entrySet().stream()
        .map(entry -> {
          Map<String, Object> summaryData = new HashMap<>();
          summaryData.put("productId", entry.getKey());
          summaryData.put("quantity", entry.getValue());
          summaryData.put("date", "total");
          return summaryData;
        })
        .sorted((a, b) -> Integer.compare((Integer) b.get("quantity"), (Integer) a.get("quantity")))
        .collect(Collectors.toList());
    
    Map<String, Object> result = new HashMap<>();
    result.put("dailyProductSales", dailyProductSales);
    result.put("totalSummary", totalSummary);
    return result;
  }

  @Override
  public Map<String, List<ProductSalesSummary>> generateTypedAggregationData(List<Order> orders) {
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    Map<String, Map<String, Integer>> dailyProductQuantity = new HashMap<>();
    Map<String, Map<String, Double>> dailyProductRevenue = new HashMap<>();
    
    // Aggregate quantity and revenue by productId and date
    for (Order order : orders) {
      String date = order.getTimestamp().format(dateFormatter);
      String orderProductId = order.getProductId();
      
      dailyProductQuantity
          .computeIfAbsent(date, k -> new HashMap<>())
          .merge(orderProductId, order.getQuantity(), Integer::sum);
      
      dailyProductRevenue
          .computeIfAbsent(date, k -> new HashMap<>())
          .merge(orderProductId, order.getTotalPrice(), Double::sum);
    }
    
    // Convert to dailyProductSales list format
    List<ProductSalesSummary> dailyProductSales = new ArrayList<>();
    for (Map.Entry<String, Map<String, Integer>> dateEntry : dailyProductQuantity.entrySet()) {
      String date = dateEntry.getKey();
      for (Map.Entry<String, Integer> productEntry : dateEntry.getValue().entrySet()) {
        String productId = productEntry.getKey();
        Double revenue = dailyProductRevenue.get(date).get(productId);
        
        dailyProductSales.add(ProductSalesSummary.builder()
            .productId(productId)
            .quantity(productEntry.getValue())
            .date(date)
            .totalRevenue(revenue)
            .build());
      }
    }
    
    // Sort dailyProductSales by date descending
    dailyProductSales.sort((a, b) -> b.getDate().compareTo(a.getDate()));
    
    // Calculate totalSummary
    Map<String, Integer> productTotals = new HashMap<>();
    Map<String, Double> productRevenueTotals = new HashMap<>();
    for (Order order : orders) {
      productTotals.merge(order.getProductId(), order.getQuantity(), Integer::sum);
      productRevenueTotals.merge(order.getProductId(), order.getTotalPrice(), Double::sum);
    }
    
    // Convert totalSummary to list format
    List<ProductSalesSummary> totalSummary = productTotals.entrySet().stream()
        .map(entry -> ProductSalesSummary.builder()
            .productId(entry.getKey())
            .quantity(entry.getValue())
            .date("total")
            .totalRevenue(productRevenueTotals.get(entry.getKey()))
            .build())
        .sorted((a, b) -> Integer.compare(b.getQuantity(), a.getQuantity()))
        .collect(Collectors.toList());
    
    Map<String, List<ProductSalesSummary>> result = new HashMap<>();
    result.put("dailyProductSales", dailyProductSales);
    result.put("totalSummary", totalSummary);
    return result;
  }

  @Override
  public GetOrdersResponse buildGetOrdersResponse(
      Page<Order> orderPage,
      String sellerId,
      String productId,
      String category,
      LocalDateTime startTime,
      LocalDateTime endTime) {
    
    // Generate typed aggregation data
    Map<String, List<ProductSalesSummary>> aggregationData = generateTypedAggregationData(orderPage.getContent());
    
    // Build response
    GetOrdersResponse.GetOrdersResponseBuilder builder = GetOrdersResponse.builder()
        .orders(orderPage.getContent())
        .startTime(startTime)
        .endTime(endTime)
        .currentPage(orderPage.getNumber())
        .totalItems(orderPage.getTotalElements())
        .totalPages(orderPage.getTotalPages())
        .dailyProductSales(aggregationData.get("dailyProductSales"))
        .totalSummary(aggregationData.get("totalSummary"));
    
    // Add optional filters
    if (sellerId != null) {
      builder.sellerId(sellerId);
    }
    if (productId != null) {
      builder.productId(productId);
    }
    if (category != null) {
      builder.category(category);
    }
    
    return builder.build();
  }
}