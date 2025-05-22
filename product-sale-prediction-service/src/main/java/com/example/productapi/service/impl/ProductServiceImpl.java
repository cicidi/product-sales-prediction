package com.example.productapi.service.impl;

import com.example.productapi.dto.SimilarProductSearchRequest;
import com.example.productapi.model.Product;
import com.example.productapi.repository.ProductRepository;
import com.example.productapi.service.EmbeddingService;
import com.example.productapi.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final EmbeddingService embeddingService;
    
    @Autowired
    public ProductServiceImpl(ProductRepository productRepository, EmbeddingService embeddingService) {
        this.productRepository = productRepository;
        this.embeddingService = embeddingService;
    }
    
    /**
     * Get all products, optionally filtered by category and/or seller ID
     */
    @Override
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
    @Override
    public Optional<Product> getProductById(String id) {
        return productRepository.findById(id);
    }
    
    /**
     * Find similar products based on product ID or description
     */
    @Override
    public List<Product> findSimilarProducts(SimilarProductSearchRequest request) {
        if (request == null) {
            return Collections.emptyList();
        }
        
        List<String> similarProductIds;
        
        // If product ID is provided, use its embedding
        if (request.getProductId() != null && !request.getProductId().isEmpty()) {
            List<Float> queryEmbedding = embeddingService.getEmbedding(request.getProductId());
            if (queryEmbedding == null) {
                return Collections.emptyList();
            }
            similarProductIds = embeddingService.findSimilarProducts(request.getProductId(), 3);
        }
        // Otherwise, generate embedding from the description text
        else if (request.getDescription() != null && !request.getDescription().isEmpty()) {
            similarProductIds = embeddingService.findSimilarProducts(request.getDescription(), 3);
        } else {
            return Collections.emptyList();
        }
        
        // Retrieve the actual product objects
        return similarProductIds.stream()
                .map(productRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * Create new product
     */
    @Override
    public Product createProduct(Map<String, Object> productData) {
        // Validate required fields
        List<String> requiredFields = Arrays.asList("name", "category", "brand", "price", "sellerId");
        for (String field : requiredFields) {
            if (!productData.containsKey(field) || productData.get(field) == null) {
                throw new IllegalArgumentException(field + " is required");
            }
        }

        // Generate a unique product ID (P + timestamp + random number)
        String productId = "P" + System.currentTimeMillis() + 
                          String.format("%04d", new Random().nextInt(10000));

        // Create product entity
        Product product = Product.builder()
                .id(productId)
                .name(productData.get("name").toString())
                .category(productData.get("category").toString())
                .brand(productData.get("brand").toString())
                .price(Double.parseDouble(productData.get("price").toString()))
                .sellerId(productData.get("sellerId").toString())
                .description(productData.getOrDefault("description", "").toString())
                .createTimestamp(LocalDateTime.now())
                .build();

        // Save product
        product = productRepository.save(product);

        // Generate and store embedding if description is provided
        if (product.getDescription() != null && !product.getDescription().isEmpty()) {
            try {
                List<Float> embedding = embeddingService.generateEmbedding(product.getDescription());
                product.setDescriptionVector(embedding);
                embeddingService.storeEmbedding(product.getId(), product.getDescription());
            } catch (Exception e) {
                // Log error but don't fail the product creation
                log.error("Error generating embedding for product {}: {}", product.getId(), e.getMessage());
            }
        }

        return product;
    }
    
    /**
     * Update product
     */
    @Override
    public Product updateProduct(String id, Map<String, Object> productData) {
        // Find existing product
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        // Update fields if provided
        if (productData.containsKey("name")) {
            existingProduct.setName(productData.get("name").toString());
        }
        if (productData.containsKey("category")) {
            existingProduct.setCategory(productData.get("category").toString());
        }
        if (productData.containsKey("brand")) {
            existingProduct.setBrand(productData.get("brand").toString());
        }
        if (productData.containsKey("price")) {
            existingProduct.setPrice(Double.parseDouble(productData.get("price").toString()));
        }
        if (productData.containsKey("description")) {
            String newDescription = productData.get("description").toString();
            existingProduct.setDescription(newDescription);

            // Update embedding if description changed
            try {
                List<Float> newEmbedding = embeddingService.generateEmbedding(newDescription);
                existingProduct.setDescriptionVector(newEmbedding);
                embeddingService.storeEmbedding(existingProduct.getId(), newDescription);
            } catch (Exception e) {
                // Log error but don't fail the update
                log.error("Error updating embedding for product {}: {}", existingProduct.getId(), e.getMessage());
            }
        }
        if (productData.containsKey("sellerId")) {
            existingProduct.setSellerId(productData.get("sellerId").toString());
        }

        // Save and return updated product
        return productRepository.save(existingProduct);
    }
}