package com.example.productapi.service;

import com.example.productapi.model.Product;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/*
 * Service interface for product operations
 */
public interface ProductService {

    /**
     * Get all products, optionally filtered by category and/or seller ID
     */
    List<Product> getAllProducts(String category, String sellerId);
    
    /**
     * Get product by ID
     */
    Optional<Product> getProductById(String id);
    
    /**
     * Create new product
     * 
     * @param productData Product data mapping:
     *                    - name: Product name
     *                    - category: Product category
     *                    - brand: Product brand
     *                    - price: Product price
     *                    - description: Product description (optional)
     *                    - sellerId: Seller ID
     * @return Created product
     */
    Product createProduct(Map<String, Object> productData);
    
    /**
     * Update product
     * 
     * @param id Product ID to update
     * @param productData Mapping of product fields to update
     * @return Updated product, returns null if product doesn't exist
     */
    Product updateProduct(String id, Map<String, Object> productData);
} 