package com.example.productapi.service.impl;

import com.example.productapi.dto.TopSellingProductResponse;
import com.example.productapi.model.Order;
import com.example.productapi.model.Product;
import com.example.productapi.repository.OrderRepository;
import com.example.productapi.repository.ProductRepository;
import com.example.productapi.service.OrderService;
import com.example.productapi.service.SalesAnalyticsService;
import com.example.productapi.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SalesAnalyticsServiceImpl implements SalesAnalyticsService {

  private final OrderRepository orderRepository;
  private final ProductRepository productRepository;

  @Autowired
  public SalesAnalyticsServiceImpl(OrderRepository orderRepository,
      ProductRepository productRepository) {
    this.orderRepository = orderRepository;
    this.productRepository = productRepository;
  }

  @Override
  public List<TopSellingProductResponse> getTopSellingProducts(
      String sellerId,
      String category,
      LocalDateTime startTime,
      LocalDateTime endTime,
      int topN,
      boolean includeRevenue) {

    // Validate input
    if (startTime == null) {
      throw new IllegalArgumentException("startTime is required");
    }

    if (endTime == null) {
      throw new IllegalArgumentException("endTime is required");
    }

    if (topN <= 0) {
      throw new IllegalArgumentException("topN must be greater than 0");
    }

    // Get top products
    List<Object[]> topResults = orderRepository.findTopProductsWithFilters(
        sellerId,
        category,
        startTime,
        endTime,
        PageRequest.of(0, topN)
    );

    // Convert results to response format
    return topResults.stream().map(result -> {
      String productId = (String) result[0];
      Long totalQuantity = (Long) result[1];
      Double totalRevenue = (Double) result[2];

      TopSellingProductResponse.TopSellingProductResponseBuilder builder = 
          TopSellingProductResponse.builder()
              .productId(productId)
              .totalQuantity(totalQuantity);

      if (includeRevenue) {
        builder.totalRevenue(totalRevenue);
      }

      // Add product details if available
      productRepository.findById(productId).ifPresent(product -> {
        builder
            .name(product.getName())
            .category(product.getCategory())
            .brand(product.getBrand())
            .price(product.getPrice());
      });

      return builder.build();
    }).collect(Collectors.toList());
  }
} 