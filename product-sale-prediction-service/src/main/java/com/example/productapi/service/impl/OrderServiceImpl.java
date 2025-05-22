package com.example.productapi.service.impl;

import com.example.productapi.model.Order;
import com.example.productapi.repository.OrderRepository;
import com.example.productapi.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
}