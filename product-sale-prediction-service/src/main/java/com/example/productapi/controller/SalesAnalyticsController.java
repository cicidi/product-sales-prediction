package com.example.productapi.controller;

import com.example.productapi.dto.SalesSearchRequest;
import com.example.productapi.dto.PredictionRequest;
import com.example.productapi.dto.TopSellingProductResponse;
import com.example.productapi.model.Product;
import com.example.productapi.model.Predications;
import com.example.productapi.service.OrderService;
import com.example.productapi.service.ProductService;
import com.example.productapi.service.SalesAnalyticsService;
import com.example.productapi.service.PredictionService;
import com.example.productapi.util.TimeUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/sales")
@Tag(name = "Sales Analytics", description = "Sales analytics endpoints")
public class SalesAnalyticsController {

    private final OrderService orderService;
    private final ProductService productService;
    private final SalesAnalyticsService salesAnalyticsService;
    private final PredictionService predictionService;
    
    @Autowired
    public SalesAnalyticsController(OrderService orderService,
                          ProductService productService, 
                          SalesAnalyticsService salesAnalyticsService,
                          PredictionService predictionService) {
        this.orderService = orderService;
        this.productService = productService;
        this.salesAnalyticsService = salesAnalyticsService;
        this.predictionService = predictionService;
    }
    
    @Operation(
        summary = "Analytics for top selling products",
        description = "Find top selling products by time range, category, and optionally filtered by seller ID"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Search results",
        content = @Content(mediaType = "application/json")
    )
    @PostMapping("/analytics")
    public ResponseEntity<Map<String, Object>> searchTopSellingProducts(@RequestBody SalesSearchRequest request) {
        // Validate request
        if (request.getTopN() == null || request.getTopN() <= 0) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "topN parameter is required and must be greater than 0")
            );
        }
        
        if (request.getStartTime() == null || request.getStartTime().isEmpty()) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "startTime parameter is required")
            );
        }
        
        // Parse start time
        LocalDateTime startTime;
        try {
            startTime = TimeUtils.parseDate(request.getStartTime());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                Map.of("error", e.getMessage())
            );
        }
        
        // Parse end time or use current time
        LocalDateTime endTime;
        if (request.getEndTime() != null && !request.getEndTime().isEmpty()) {
            try {
                endTime = TimeUtils.parseDateEndOfDay(request.getEndTime());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
                );
            }
        } else {
            endTime = LocalDateTime.now();
        }
        
        // Get top selling products
        List<TopSellingProductResponse> topProducts = salesAnalyticsService.getTopSellingProducts(
            request.getSellerId(), // Now optional
            request.getCategory(),
            startTime,
            endTime,
            request.getTopN(),
            true // Include revenue information
        );
        
        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("products", topProducts);
        response.put("count", topProducts.size());
        response.put("query", request);
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Predict product sales",
        description = "Predict future sales for a specific product using historical data and ML model"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Sales prediction",
        content = @Content(mediaType = "application/json")
    )
    @PostMapping("/predict")
    public ResponseEntity<?> predictProductSales(@RequestBody PredictionRequest request) {
        // Validate request
        if (request.getProductId() == null || request.getProductId().isEmpty()) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "productId parameter is required")
            );
        }
        
        if (request.getSellerId() == null || request.getSellerId().isEmpty()) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "sellerId parameter is required")
            );
        }
        
        if (request.getStartDate() == null) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "startDate parameter is required")
            );
        }
        
        // Get product information
        Optional<Product> productOpt = productService.getProductById(request.getProductId());
        if (productOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "Product not found: " + request.getProductId())
            );
        }
        
        // Make prediction
        try {
            Predications predictions = predictionService.predictSales(
                request.getProductId(), 
                request.getSellerId(), 
                request.getSalePrice(),
                request.getStartDate(), 
                request.getEndDate()
            );
            
            return ResponseEntity.ok(predictions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "Prediction error: " + e.getMessage())
            );
        }
    }
} 