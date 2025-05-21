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
    
    Page<Order> findAllByOrderByTimestampDesc(Pageable pageable);
    
    Page<Order> findBySellerIdOrderByTimestampDesc(String sellerId, Pageable pageable);
    
    Page<Order> findBySellerIdAndTimestampBetweenOrderByTimestampDesc(
            String sellerId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    boolean existsBySellerId(String sellerId);
    
    List<Order> findByProductId(String productId);
    
    List<Order> findBySellerIdAndTimestampBetween(String sellerId, LocalDateTime startTime, LocalDateTime endTime);
    
    List<Order> findBySellerIdAndProductIdAndTimestampBetween(
            String sellerId, String productId, LocalDateTime startTime, LocalDateTime endTime);
    
    @Query("SELECT o.productId, SUM(o.quantity) as totalQuantity " +
           "FROM Order o " +
           "WHERE o.sellerId = :sellerId " +
           "AND o.timestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY o.productId " +
           "ORDER BY totalQuantity DESC")
    List<Object[]> findTopProductsBySellerId(String sellerId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    @Query("SELECT o.productId, SUM(o.quantity) as totalQuantity " +
           "FROM Order o JOIN Product p ON o.productId = p.id " +
           "JOIN Inventory i ON p.id = i.productId AND i.sellerId = o.sellerId " +
           "WHERE o.sellerId = :sellerId " +
           "AND o.timestamp BETWEEN :startTime AND :endTime " +
           "AND p.category = :category " +
           "GROUP BY o.productId " +
           "ORDER BY totalQuantity DESC")
    List<Object[]> findTopProductsBySellerIdAndCategory(
            String sellerId, String category, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
} 