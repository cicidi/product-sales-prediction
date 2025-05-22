package com.example.productapi.service.impl;

import com.example.productapi.dto.SimilarProductSearchRequest;
import com.example.productapi.model.Product;
import com.example.productapi.service.ProductService;
import com.example.productapi.service.ProductSimilarityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductSimilarityServiceImpl implements ProductSimilarityService {

    private final ProductService productService;
    
    @Autowired
    public ProductSimilarityServiceImpl(ProductService productService) {
        this.productService = productService;
    }
    
    /**
     * Find similar products
     */
    @Override
    public Map<String, Object> findSimilarProducts(Map<String, Object> request) {
        // Create search request object
        SimilarProductSearchRequest searchRequest = new SimilarProductSearchRequest();
        
        // Set description text (if provided)
        if (request.containsKey("description")) {
            searchRequest.setDescription(request.get("description").toString());
        }
        
        // Set product ID (if provided)
        if (request.containsKey("productId")) {
            searchRequest.setProductId(request.get("productId").toString());
        }
        
        // Get result limit (if provided)
        int limit = 10; // Default limit is 10 results
        if (request.containsKey("limit")) {
            try {
                limit = Integer.parseInt(request.get("limit").toString());
            } catch (NumberFormatException e) {
                // Use default value
            }
        }
        
        // Call product service to find similar products
        List<Product> similarProducts = productService.findSimilarProducts(searchRequest);
        
        // Apply limit (if needed)
        if (similarProducts.size() > limit) {
            similarProducts = similarProducts.subList(0, limit);
        }
        
        // Build response
        Map<String, Object> result = new HashMap<>();
        result.put("products", similarProducts);
        result.put("count", similarProducts.size());
        
        return result;
    }
} 