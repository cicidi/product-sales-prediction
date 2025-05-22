package com.example.productapi.repository;

import com.example.productapi.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    
    /**
     * Find all orders sorted by timestamp in descending order, with pagination
     */
    Page<Order> findAllByOrderByTimestampDesc(Pageable pageable);
    
    /**
     * Find orders by seller ID, with pagination
     */
    Page<Order> findBySellerIdOrderByTimestampDesc(String sellerId, Pageable pageable);
    
    /**
     * Find orders by seller ID and time range, with pagination
     */
    Page<Order> findBySellerIdAndTimestampBetweenOrderByTimestampDesc(
            String sellerId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    /**
     * Check if orders exist for a specific seller
     */
    boolean existsBySellerId(String sellerId);
    
    /**
     * Find orders by seller ID and time range
     */
    List<Order> findBySellerIdAndTimestampBetween(String sellerId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Find orders by seller ID, product ID and time range
     */
    List<Order> findBySellerIdAndProductIdAndTimestampBetween(
            String sellerId, String productId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Find orders by seller ID, product ID list and time range
     */
    List<Order> findBySellerIdAndProductIdInAndTimestampBetween(
            String sellerId, List<String> productIds, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Find orders by seller ID, product ID and time range (after specified time)
     */
    List<Order> findBySellerIdAndProductIdAndTimestampAfter(
            String sellerId, String productId, LocalDateTime startTime);
    
    List<Order> findByProductId(String productId);
    
    @Query("SELECT o.productId, SUM(o.quantity) as totalQuantity, SUM(o.quantity * o.unitPrice) as totalRevenue " +
           "FROM Order o " +
           "WHERE o.sellerId = :sellerId " +
           "AND o.timestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY o.productId " +
           "ORDER BY totalQuantity DESC")
    List<Object[]> findTopProductsBySellerId(String sellerId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    /**
     * Find orders with filters
     */
    @Query("SELECT o FROM Order o JOIN Product p ON o.productId = p.id " +
           "WHERE (:sellerId IS NULL OR o.sellerId = :sellerId) " +
           "AND (:productId IS NULL OR o.productId = :productId) " +
           "AND (:category IS NULL OR p.category = :category) " +
           "AND o.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY o.timestamp DESC")
    Page<Order> findOrdersWithFilters(
            String sellerId,
            String productId,
            String category,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Pageable pageable);

    /**
     * Find orders with filters (without pagination)
     */
    @Query("SELECT o FROM Order o JOIN Product p ON o.productId = p.id " +
           "WHERE (:sellerId IS NULL OR o.sellerId = :sellerId) " +
           "AND (:productId IS NULL OR o.productId = :productId) " +
           "AND (:category IS NULL OR p.category = :category) " +
           "AND o.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY o.timestamp DESC")
    List<Order> findOrdersWithFiltersNoPaging(
            String sellerId,
            String productId,
            String category,
            LocalDateTime startTime,
            LocalDateTime endTime);

    /**
     * Find top selling products with filters
     */
    @Query("SELECT o.productId, SUM(o.quantity) as totalQuantity, SUM(o.quantity * o.unitPrice) as totalRevenue " +
           "FROM Order o JOIN Product p ON o.productId = p.id " +
           "WHERE (:sellerId IS NULL OR o.sellerId = :sellerId) " +
           "AND (:category IS NULL OR p.category = :category) " +
           "AND o.timestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY o.productId " +
           "ORDER BY totalQuantity DESC")
    List<Object[]> findTopProductsWithFilters(
            String sellerId,
            String category,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Pageable pageable);

    /**
     * Find all orders by seller ID and product ID (without time limit)
     */
    List<Order> findBySellerIdAndProductId(String sellerId, String productId);
} 