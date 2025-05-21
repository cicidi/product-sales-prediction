package com.example.productapi.repository;

import com.example.productapi.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    
    List<Product> findByCategory(String category);
    
    @Query("SELECT p FROM Product p JOIN Inventory i ON p.id = i.productId WHERE i.sellerId = :sellerId")
    List<Product> findBySellerId(String sellerId);
    
    @Query("SELECT p FROM Product p JOIN Inventory i ON p.id = i.productId WHERE i.sellerId = :sellerId AND p.category = :category")
    List<Product> findBySellerIdAndCategory(String sellerId, String category);
} 