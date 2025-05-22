package com.example.productapi.service;

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
}