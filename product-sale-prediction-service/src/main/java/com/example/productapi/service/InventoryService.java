//package com.example.productapi.service;
//
//import com.example.productapi.model.Inventory;
//import java.util.List;
//import java.util.Optional;
//
///**
// * 库存服务接口
// */
//public interface InventoryService {
//
//    /**
//     * Get inventory by product ID and seller ID
//     *
//     * @param productId Product ID
//     * @param sellerId Seller ID
//     * @return Optional inventory record
//     */
//    Optional<Inventory> getInventoryByProductAndSeller(String productId, String sellerId);
//
//    /**
//     * Get all inventories by seller ID
//     *
//     * @param sellerId Seller ID
//     * @return List of inventory records for the seller
//     */
//    List<Inventory> getInventoriesBySeller(String sellerId);
//
//    /**
//     * Get all inventories by product ID
//     *
//     * @param productId Product ID
//     * @return List of inventory records for the product
//     */
//    List<Inventory> getInventoriesByProduct(String productId);
//
//    /**
//     * Get all inventories with optional filters
//     *
//     * @param productId Optional product ID filter
//     * @param sellerId Optional seller ID filter
//     * @return List of filtered inventory records
//     */
//    List<Inventory> getInventoriesWithFilters(String productId, String sellerId);
//}