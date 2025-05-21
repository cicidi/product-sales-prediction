package com.example.productapi.repository;

import com.example.productapi.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    
    List<Inventory> findByProductId(String productId);
    
    List<Inventory> findBySellerId(String sellerId);
    
    Optional<Inventory> findByProductIdAndSellerId(String productId, String sellerId);
    
    List<Inventory> findBySellerIdAndProductIdIn(String sellerId, List<String> productIds);
    
    boolean existsByProductIdAndSellerId(String productId, String sellerId);
} 