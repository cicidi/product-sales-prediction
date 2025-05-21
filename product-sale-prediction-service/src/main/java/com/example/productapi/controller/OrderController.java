package com.example.productapi.controller;

import com.example.productapi.mcp.model.ToolResponse;
import com.example.productapi.model.Order;
import com.example.productapi.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1")
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {

    private final OrderService orderService;
    
    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    
    @Operation(
        summary = "Get recent orders",
        description = "Retrieve recent orders with pagination support (max 100 items per page)"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Successfully retrieved orders",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = Order.class))
    )
    @ApiResponse(
        responseCode = "400", 
        description = "Bad request - sellerId is missing"
    )
    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> getRecentOrders(
            @Parameter(description = "Seller ID (required to filter orders by seller)", required = true) 
            @RequestParam String sellerId,
            
            @Parameter(description = "Page number (0-based)") 
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size (max 100)") 
            @RequestParam(defaultValue = "20") int size) {
        
        // Validate sellerId
        if (sellerId == null || sellerId.trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "sellerId parameter is required");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        // Enforce max size of 100
        if (size > 100) {
            size = 100;
        }
        
        Page<Order> orderPage = orderService.getOrdersBySeller(sellerId, page, size);
        
        Map<String, Object> response = new HashMap<>();
        response.put("orders", orderPage.getContent());
        response.put("currentPage", orderPage.getNumber());
        response.put("totalItems", orderPage.getTotalElements());
        response.put("totalPages", orderPage.getTotalPages());
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Get orders by seller ID",
        description = "Retrieve all orders for a specific seller with pagination support"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Successfully retrieved orders",
        content = @Content(mediaType = "application/json")
    )
    @ApiResponse(
        responseCode = "404", 
        description = "Seller not found"
    )
    @GetMapping("/orders/seller/{sellerId}")
    public ResponseEntity<Map<String, Object>> getOrdersBySellerId(
            @Parameter(description = "Seller ID", required = true) 
            @PathVariable String sellerId,
            
            @Parameter(description = "Filter orders from this date (ISO format)") 
            @RequestParam(required = false) String startDate,
            
            @Parameter(description = "Filter orders until this date (ISO format)") 
            @RequestParam(required = false) String endDate,
            
            @Parameter(description = "Page number (0-based)") 
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size (max 100)") 
            @RequestParam(defaultValue = "20") int size) {
        
        // Enforce max size of 100
        if (size > 100) {
            size = 100;
        }
        
        // Parse dates if provided
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;
        
        if (startDate != null && !startDate.isEmpty()) {
            LocalDate date = LocalDate.parse(startDate);
            startDateTime = date.atStartOfDay();
        }
        
        if (endDate != null && !endDate.isEmpty()) {
            LocalDate date = LocalDate.parse(endDate);
            endDateTime = date.atTime(LocalTime.MAX);
        }
        
        Page<Order> orderPage;
        if (startDateTime != null && endDateTime != null) {
            orderPage = orderService.getOrdersBySellerAndDateRange(sellerId, startDateTime, endDateTime, page, size);
        } else {
            orderPage = orderService.getOrdersBySeller(sellerId, page, size);
        }
        
        // Check if seller exists by checking if any orders were found in the database
        if (orderPage.getTotalElements() == 0 && !orderService.sellerExists(sellerId)) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Seller not found");
            return ResponseEntity.status(404).body(errorResponse);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("orders", orderPage.getContent());
        response.put("sellerId", sellerId);
        response.put("currentPage", orderPage.getNumber());
        response.put("totalItems", orderPage.getTotalElements());
        response.put("totalPages", orderPage.getTotalPages());
        
        if (startDateTime != null || endDateTime != null) {
            Map<String, String> dateRange = new HashMap<>();
            if (startDateTime != null) {
                dateRange.put("start", startDate);
            }
            if (endDateTime != null) {
                dateRange.put("end", endDate);
            }
            response.put("dateRange", dateRange);
        }
        
        return ResponseEntity.ok(response);
    }
    
    // MCP-compliant endpoint to expose as a tool for LLMs
    @Operation(
        summary = "MCP Tool: Get recent orders",
        description = "LLM-friendly endpoint to retrieve recent orders"
    )
    @PostMapping("/mcp/get-recent-orders")
    public ResponseEntity<ToolResponse> getRecentOrdersMCP(
            @RequestBody Map<String, Object> parameters) {
        
        // Extract and validate sellerId
        String sellerId = parameters.containsKey("sellerId") ? parameters.get("sellerId").toString() : null;
        if (sellerId == null || sellerId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                ToolResponse.error("getRecentOrders", "sellerId parameter is required")
            );
        }
        
        int page = parameters.containsKey("page") ? Integer.parseInt(parameters.get("page").toString()) : 0;
        int size = parameters.containsKey("size") ? Integer.parseInt(parameters.get("size").toString()) : 20;
        
        // Enforce max size of 100
        if (size > 100) {
            size = 100;
        }
        
        Page<Order> orderPage = orderService.getOrdersBySeller(sellerId, page, size);
        
        Map<String, Object> result = new HashMap<>();
        result.put("orders", orderPage.getContent());
        result.put("sellerId", sellerId);
        result.put("currentPage", orderPage.getNumber());
        result.put("totalItems", orderPage.getTotalElements());
        result.put("totalPages", orderPage.getTotalPages());
        
        return ResponseEntity.ok(ToolResponse.success("getRecentOrders", result));
    }
} 