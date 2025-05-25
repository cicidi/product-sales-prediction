package com.example.productapi.controller;

import com.example.productapi.dto.SalesSearchRequest;
import com.example.productapi.dto.PredictionRequest;
import com.example.productapi.dto.TopSellingProductResponse;
import com.example.productapi.dto.ProductSalesSummary;
import com.example.productapi.dto.SalesAnalyticsResponse;
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
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
        summary = "Sales analytics with daily and total summaries",
        description = "Get daily product sales summary and total summary for a time range. If topN is provided, returns only top N products by total sales."
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Sales analytics results",
        content = @Content(mediaType = "application/json")
    )
    @PostMapping("/analytics")
    public ResponseEntity<SalesAnalyticsResponse> searchTopSellingProducts(@RequestBody SalesSearchRequest request) {
        // Validate request - topN is now optional
        if (request.getStartTime() == null || request.getStartTime().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        // Parse start time
        LocalDateTime startTime;
        try {
            startTime = TimeUtils.parseDate(request.getStartTime());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        
        // Parse end time or use current time
        LocalDateTime endTime;
        if (request.getEndTime() != null && !request.getEndTime().isEmpty()) {
            try {
                endTime = TimeUtils.parseDateEndOfDay(request.getEndTime());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        } else {
            endTime = LocalDateTime.now();
        }

        // Get all orders for the time range
        List<com.example.productapi.model.Order> orders = orderService.getOrdersWithFilters(
            request.getSellerId(),
            request.getProductId(),
            request.getCategory(),
            startTime,
            endTime,
            0,
            Integer.MAX_VALUE
        ).getContent();

        // Generate aggregation data
        Map<String, List<ProductSalesSummary>> aggregationData = orderService.generateTypedAggregationData(orders);
        
        List<ProductSalesSummary> dailyProductSales = aggregationData.get("dailyProductSales");
        List<ProductSalesSummary> totalSummary = aggregationData.get("totalSummary");

        // Apply topN filter if provided
        if (request.getTopN() != null && request.getTopN() > 0) {
            // Get top N products by total sales
            List<String> topProductIds = totalSummary.stream()
                .limit(request.getTopN())
                .map(ProductSalesSummary::getProductId)
                .collect(Collectors.toList());
            
            // Filter daily sales to only include top N products
            dailyProductSales = dailyProductSales.stream()
                .filter(summary -> topProductIds.contains(summary.getProductId()))
                .collect(Collectors.toList());
            
            // Limit total summary to top N
            totalSummary = totalSummary.stream()
                .limit(request.getTopN())
                .collect(Collectors.toList());
        }

        // Build response
        SalesAnalyticsResponse.SalesAnalyticsResponseBuilder builder = SalesAnalyticsResponse.builder()
            .dailyProductSales(dailyProductSales)
            .totalSummary(totalSummary)
            .startTime(startTime)
            .endTime(endTime);
        
        // Add filter information to response
        if (request.getSellerId() != null) {
            builder.sellerId(request.getSellerId());
        }
        if (request.getProductId() != null) {
            builder.productId(request.getProductId());
        }
        if (request.getCategory() != null) {
            builder.category(request.getCategory());
        }
        if (request.getTopN() != null) {
            builder.topN(request.getTopN());
        }

        return ResponseEntity.ok(builder.build());
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