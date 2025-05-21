package com.example.productapi.service;

import com.example.productapi.model.Order;
import com.example.productapi.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    
    @Autowired
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
    
    /**
     * Get recent orders with pagination
     * @deprecated Use {@link #getOrdersBySeller(String, int, int)} instead
     */
    @Deprecated
    public Page<Order> getRecentOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orderRepository.findAllByOrderByTimestampDesc(pageable);
    }
    
    /**
     * Get orders by seller ID with pagination
     */
    public Page<Order> getOrdersBySeller(String sellerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orderRepository.findBySellerIdOrderByTimestampDesc(sellerId, pageable);
    }
    
    /**
     * Get orders by seller ID and date range with pagination
     */
    public Page<Order> getOrdersBySellerAndDateRange(
            String sellerId, LocalDateTime startTime, LocalDateTime endTime, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orderRepository.findBySellerIdAndTimestampBetweenOrderByTimestampDesc(
            sellerId, startTime, endTime, pageable);
    }
    
    /**
     * Check if seller exists
     */
    public boolean sellerExists(String sellerId) {
        // Check if there are any orders with this seller ID
        return orderRepository.existsBySellerId(sellerId);
    }
    
    /**
     * Parse date string in format yyyy/MM to LocalDateTime
     */
    public LocalDateTime parseYearMonth(String yearMonthStr) {
        try {
            // Parse yyyy/MM format
            String[] parts = yearMonthStr.split("/");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            
            // Create first day of the month
            return LocalDateTime.of(year, month, 1, 0, 0);
        } catch (DateTimeParseException | ArrayIndexOutOfBoundsException | NumberFormatException e) {
            throw new IllegalArgumentException("Invalid date format. Expected yyyy/MM, got: " + yearMonthStr);
        }
    }
    
    /**
     * Get orders by seller ID and date range
     */
    public List<Order> getOrdersBySellerAndDateRange(String sellerId, LocalDateTime startTime, LocalDateTime endTime) {
        return orderRepository.findBySellerIdAndTimestampBetween(sellerId, startTime, endTime);
    }
    
    /**
     * Get orders by seller ID, product ID and date range
     */
    public List<Order> getOrdersBySellerAndProductAndDateRange(
            String sellerId, String productId, LocalDateTime startTime, LocalDateTime endTime) {
        return orderRepository.findBySellerIdAndProductIdAndTimestampBetween(sellerId, productId, startTime, endTime);
    }
} 