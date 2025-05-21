package com.example.productapi.service;

import com.example.productapi.dto.SimilarProductSearchRequest;
import com.example.productapi.model.Product;
import com.example.productapi.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final EmbeddingService embeddingService;
    
    @Autowired
    public ProductService(ProductRepository productRepository, EmbeddingService embeddingService) {
        this.productRepository = productRepository;
        this.embeddingService = embeddingService;
    }
    
    /**
     * Get all products, optionally filtered by category and/or seller ID
     */
    public List<Product> getAllProducts(String category, String sellerId) {
        // If no filters provided, return all products
        if ((category == null || category.trim().isEmpty()) && (sellerId == null || sellerId.trim().isEmpty())) {
            return productRepository.findAll();
        }
        
        // If only category filter provided
        if (sellerId == null || sellerId.trim().isEmpty()) {
            return productRepository.findByCategory(category);
        }
        
        // If only seller ID filter provided
        if (category == null || category.trim().isEmpty()) {
            return productRepository.findBySellerId(sellerId);
        }
        
        // Both filters provided
        return productRepository.findBySellerIdAndCategory(sellerId, category);
    }
    
    /**
     * Get product by ID
     */
    public Optional<Product> getProductById(String id) {
        return productRepository.findById(id);
    }
    
    /**
     * Find similar products based on product ID or description
     */
    public List<Product> findSimilarProducts(SimilarProductSearchRequest request) {
        if (request == null) {
            return Collections.emptyList();
        }
        
        // Get the embedding to compare against
        List<Float> queryEmbedding = null;
        
        // If product ID is provided, use its embedding
        if (request.getProductId() != null && !request.getProductId().isEmpty()) {
            queryEmbedding = embeddingService.getProductEmbedding(request.getProductId());
        }
        // Otherwise, generate embedding from the description text
        else if (request.getDescription() != null && !request.getDescription().isEmpty()) {
            queryEmbedding = embeddingService.generateEmbedding(request.getDescription());
        }
        
        // If we couldn't get an embedding, return empty list
        if (queryEmbedding == null) {
            return Collections.emptyList();
        }
        
        // Find similar products based on embedding
        List<String> similarProductIds = embeddingService.findSimilarProducts(queryEmbedding, 10);
        
        // Retrieve the actual product objects
        return similarProductIds.stream()
                .map(productRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
} 