package com.example.productapi.controller;

import com.example.productapi.dto.SalesPredictionRequest;
import com.example.productapi.dto.SalesSearchRequest;
import com.example.productapi.mcp.model.ToolResponse;
import com.example.productapi.model.Product;
import com.example.productapi.service.OrderService;
import com.example.productapi.service.ProductService;
import com.example.productapi.service.SalesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Tag(name = "Sales", description = "Sales analytics and prediction endpoints")
public class SalesController {

    private final OrderService orderService;
    private final ProductService productService;
    private final SalesService salesService;
    
    @Autowired
    public SalesController(OrderService orderService, ProductService productService, SalesService salesService) {
        this.orderService = orderService;
        this.productService = productService;
        this.salesService = salesService;
    }
    
    @Operation(
        summary = "Search for top selling products",
        description = "Find top selling products by time range, category, and seller ID"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Search results",
        content = @Content(mediaType = "application/json")
    )
    @PostMapping("/search")
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
        
        if (request.getSellerId() == null || request.getSellerId().isEmpty()) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "sellerId parameter is required")
            );
        }
        
        // Parse start time
        LocalDateTime startTime;
        try {
            startTime = orderService.parseYearMonth(request.getStartTime());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                Map.of("error", e.getMessage())
            );
        }
        
        // Parse end time or use current time
        LocalDateTime endTime;
        if (request.getEndTime() != null && !request.getEndTime().isEmpty()) {
            try {
                endTime = orderService.parseYearMonth(request.getEndTime());
                // Move to the end of the month
                endTime = endTime.plusMonths(1).minusNanos(1);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
                );
            }
        } else {
            endTime = LocalDateTime.now();
        }
        
        // Get top selling products
        List<Map<String, Object>> topProducts;
        if (request.getCategory() != null && !request.getCategory().isEmpty()) {
            topProducts = salesService.getTopProductsByCategoryAndSeller(
                request.getSellerId(), request.getCategory(), startTime, endTime, request.getTopN());
        } else {
            topProducts = salesService.getTopProductsBySeller(
                request.getSellerId(), startTime, endTime, request.getTopN());
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("products", topProducts);
        response.put("count", topProducts.size());
        response.put("query", request);
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Predict product sales",
        description = "Predict future sales for a specific product"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Sales prediction",
        content = @Content(mediaType = "application/json")
    )
    @PostMapping("/predict")
    public ResponseEntity<Map<String, Object>> predictProductSales(@RequestBody SalesPredictionRequest request) {
        // Validate request
        if (request.getProductId() == null || request.getProductId().isEmpty()) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "productId parameter is required")
            );
        }
        
        if (request.getStartTime() == null || request.getStartTime().isEmpty()) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "startTime parameter is required")
            );
        }
        
        if (request.getSellerId() == null || request.getSellerId().isEmpty()) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "sellerId parameter is required")
            );
        }
        
        // Get product information
        Optional<Product> productOpt = productService.getProductById(request.getProductId());
        if (productOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Product product = productOpt.get();
        
        // Parse times
        LocalDateTime startTime;
        LocalDateTime endTime;
        
        try {
            startTime = orderService.parseYearMonth(request.getStartTime());
            
            if (request.getEndTime() != null && !request.getEndTime().isEmpty()) {
                endTime = orderService.parseYearMonth(request.getEndTime());
                // Move to the end of the month
                endTime = endTime.plusMonths(1).minusNanos(1);
            } else {
                endTime = LocalDateTime.now().plusMonths(3); // Default: predict next 3 months
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                Map.of("error", e.getMessage())
            );
        }
        
        // Make prediction
        try {
            Map<String, Object> prediction = salesService.predictProductSales(
                product, request.getSellerId(), startTime, endTime);
            
            Map<String, Object> response = new HashMap<>();
            response.put("prediction", prediction);
            response.put("product", product);
            response.put("query", request);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "Prediction error: " + e.getMessage())
            );
        }
    }
    
    // MCP-compliant endpoint to expose as a tool for LLMs
    @Operation(
        summary = "MCP Tool: Search top selling products",
        description = "LLM-friendly endpoint to find top selling products"
    )
    @PostMapping("/mcp/search-top-products")
    public ResponseEntity<ToolResponse> searchTopProductsMCP(@RequestBody Map<String, Object> parameters) {
        try {
            Integer topN = parameters.containsKey("topN") ? 
                Integer.parseInt(parameters.get("topN").toString()) : 10;
                
            String startTime = (String) parameters.get("startTime");
            String endTime = parameters.containsKey("endTime") ?
                (String) parameters.get("endTime") : null;
                
            String sellerId = (String) parameters.get("sellerId");
            String category = parameters.containsKey("category") ?
                (String) parameters.get("category") : null;
                
            SalesSearchRequest request = new SalesSearchRequest(topN, startTime, endTime, category, sellerId);
            
            // Re-use the standard endpoint logic
            ResponseEntity<Map<String, Object>> response = searchTopSellingProducts(request);
            
            // Return MCP-compliant response
            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(ToolResponse.success("searchTopProducts", response.getBody()));
            } else {
                return ResponseEntity.ok(ToolResponse.error("searchTopProducts", 
                    response.getBody() != null ? response.getBody().toString() : "Error searching top products"));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(ToolResponse.error("searchTopProducts", "Error: " + e.getMessage()));
        }
    }
    
    // MCP-compliant endpoint to expose as a tool for LLMs
    @Operation(
        summary = "MCP Tool: Predict product sales",
        description = "LLM-friendly endpoint to predict product sales"
    )
    @PostMapping("/mcp/predict-sales")
    public ResponseEntity<ToolResponse> predictSalesMCP(@RequestBody Map<String, Object> parameters) {
        try {
            String productId = (String) parameters.get("productId");
            String startTime = (String) parameters.get("startTime");
            String endTime = parameters.containsKey("endTime") ?
                (String) parameters.get("endTime") : null;
            String category = parameters.containsKey("category") ?
                (String) parameters.get("category") : null;
            String sellerId = (String) parameters.get("sellerId");
            
            SalesPredictionRequest request = new SalesPredictionRequest(productId, startTime, endTime, category, sellerId);
            
            // Re-use the standard endpoint logic
            ResponseEntity<Map<String, Object>> response = predictProductSales(request);
            
            // Return MCP-compliant response
            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(ToolResponse.success("predictSales", response.getBody()));
            } else {
                return ResponseEntity.ok(ToolResponse.error("predictSales", 
                    response.getBody() != null ? response.getBody().toString() : "Error predicting sales"));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(ToolResponse.error("predictSales", "Error: " + e.getMessage()));
        }
    }
} 