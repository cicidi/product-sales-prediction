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
     * 按时间戳降序查找所有订单，包含分页
     */
    Page<Order> findAllByOrderByTimestampDesc(Pageable pageable);
    
    /**
     * 按卖家ID查找订单，包含分页
     */
    Page<Order> findBySellerIdOrderByTimestampDesc(String sellerId, Pageable pageable);
    
    /**
     * 按卖家ID和时间范围查找订单，包含分页
     */
    Page<Order> findBySellerIdAndTimestampBetweenOrderByTimestampDesc(
            String sellerId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    /**
     * 检查是否存在特定卖家的订单
     */
    boolean existsBySellerId(String sellerId);
    
    /**
     * 按卖家ID和时间范围查找订单
     */
    List<Order> findBySellerIdAndTimestampBetween(String sellerId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 按卖家ID、商品ID和时间范围查找订单
     */
    List<Order> findBySellerIdAndProductIdAndTimestampBetween(
            String sellerId, String productId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 按卖家ID、商品ID列表和时间范围查找订单
     */
    List<Order> findBySellerIdAndProductIdInAndTimestampBetween(
            String sellerId, List<String> productIds, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 按卖家ID、商品ID和时间范围查找订单（从指定时间之后）
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
     * 按卖家ID和产品ID查找所有订单（不受时间限制）
     */
    List<Order> findBySellerIdAndProductId(String sellerId, String productId);
} 