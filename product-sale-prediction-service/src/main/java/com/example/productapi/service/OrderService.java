package com.example.productapi.service;

import com.example.productapi.dto.GetOrdersResponse;
import com.example.productapi.dto.ProductSalesSummary;
import com.example.productapi.model.Order;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 订单服务接口
 */
public interface OrderService {

  /**
   * Get orders by seller ID with pagination
   */
  Page<Order> getOrdersBySeller(String sellerId, int page, int size);

  /**
   * Get orders with filters
   *
   * @param sellerId  Optional seller ID to filter by
   * @param productId Optional product ID to filter by
   * @param category  Optional category to filter by
   * @param startTime Start time for filtering orders
   * @param endTime   End time for filtering orders
   * @param page      Page number (0-based)
   * @param size      Page size
   * @return Page of filtered orders
   */
  Page<Order> getOrdersWithFilters(
      String sellerId,
      String productId,
      String category,
      LocalDateTime startTime,
      LocalDateTime endTime,
      int page,
      int size);

  /**
   * Generate aggregation data for orders
   *
   * @param orders List of orders to aggregate
   * @return Map containing dailyProductSales and totalSummary
   */
  Map<String, Object> generateAggregationData(List<Order> orders);

  /**
   * Generate typed aggregation data for orders
   *
   * @param orders List of orders to aggregate
   * @return Map containing typed ProductSalesSummary lists
   */
  Map<String, List<ProductSalesSummary>> generateTypedAggregationData(List<Order> orders);

  /**
   * Build GetOrdersResponse with pagination and aggregation data
   *
   * @param orderPage Page of orders
   * @param sellerId Optional seller ID filter
   * @param productId Optional product ID filter
   * @param category Optional category filter
   * @param startTime Start time for filtering
   * @param endTime End time for filtering
   * @return Complete GetOrdersResponse
   */
  GetOrdersResponse buildGetOrdersResponse(
      Page<Order> orderPage,
      String sellerId,
      String productId,
      String category,
      LocalDateTime startTime,
      LocalDateTime endTime);
}