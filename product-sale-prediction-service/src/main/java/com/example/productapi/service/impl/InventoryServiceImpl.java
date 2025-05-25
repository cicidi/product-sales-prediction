//package com.example.productapi.service.impl;
//
//import com.example.productapi.model.Inventory;
//import com.example.productapi.repository.InventoryRepository;
//import com.example.productapi.service.InventoryService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.Optional;
//
//@Service
//public class InventoryServiceImpl implements InventoryService {
//
//    private final InventoryRepository inventoryRepository;
//
//    @Autowired
//    public InventoryServiceImpl(InventoryRepository inventoryRepository) {
//        this.inventoryRepository = inventoryRepository;
//    }
//
//    @Override
//    public Optional<Inventory> getInventoryByProductAndSeller(String productId, String sellerId) {
//        return inventoryRepository.findByProductIdAndSellerId(productId, sellerId);
//    }
//
//    @Override
//    public List<Inventory> getInventoriesBySeller(String sellerId) {
//        return inventoryRepository.findBySellerId(sellerId);
//    }
//
//    @Override
//    public List<Inventory> getInventoriesByProduct(String productId) {
//        return inventoryRepository.findByProductId(productId);
//    }
//
//    @Override
//    public List<Inventory> getInventoriesWithFilters(String productId, String sellerId) {
//        if (productId != null && sellerId != null) {
//            // Both filters provided, return specific inventory
//            return inventoryRepository.findByProductIdAndSellerId(productId, sellerId)
//                .map(List::of)
//                .orElse(List.of());
//        } else if (sellerId != null) {
//            // Only seller filter provided
//            return inventoryRepository.findBySellerId(sellerId);
//        } else if (productId != null) {
//            // Only product filter provided
//            return inventoryRepository.findByProductId(productId);
//        } else {
//            // No filters provided, return all
//            return inventoryRepository.findAll();
//        }
//    }
//}