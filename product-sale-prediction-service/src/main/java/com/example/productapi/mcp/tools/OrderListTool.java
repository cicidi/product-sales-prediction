package com.example.productapi.mcp.tools;

import com.example.productapi.mcp.model.ToolDefinition;
import com.example.productapi.mcp.model.ToolResponse;
import com.example.productapi.mcp.service.Tool;
import com.example.productapi.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import com.example.productapi.model.Order;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class OrderListTool implements Tool {
    
    private final OrderService orderService;
    private final ToolDefinition definition;
    
    @Autowired
    public OrderListTool(OrderService orderService) {
        this.orderService = orderService;
        
        // Build tool definition
        this.definition = ToolDefinition.builder()
                .name("get_sellers_orders")
                .displayName("Order List Query")
                .description("Query order records for a specific seller, supporting time range and pagination")
                .parameters(Arrays.asList(
                    ToolDefinition.ParameterDefinition.builder()
                        .name("seller_id")
                        .type("string")
                        .description("Seller ID, required parameter, sellers cannot view other sellers' orders")
                        .required(true)
                        .example("SELLER789")
                        .build(),
                    ToolDefinition.ParameterDefinition.builder()
                        .name("start_time")
                        .type("string")
                        .description("Start time (format: yyyy/MM/dd), optional parameter, defaults to 2025/02/01")
                        .required(false)
                        .example("2025/02/01")
                        .build(),
                    ToolDefinition.ParameterDefinition.builder()
                        .name("end_time")
                        .type("string")
                        .description("End time (format: yyyy/MM/dd), optional parameter, defaults to 2025/05/01")
                        .required(false)
                        .example("2025/05/01")
                        .build(),
                    ToolDefinition.ParameterDefinition.builder()
                        .name("page")
                        .type("integer")
                        .description("Page number, starting from 0, optional parameter, defaults to 0 (first page)")
                        .required(false)
                        .defaultValue(0)
                        .example(0)
                        .build(),
                    ToolDefinition.ParameterDefinition.builder()
                        .name("size")
                        .type("integer")
                        .description("Records per page, optional parameter, defaults to 20, maximum 100")
                        .required(false)
                        .defaultValue(20)
                        .example(20)
                        .build()
                ))
                .outputSchema(Map.of(
                    "orders", "Order list",
                    "current_page", "Current page number",
                    "total_items", "Total number of records",
                    "total_pages", "Total number of pages",
                    "start_time", "Query start time",
                    "end_time", "Query end time"
                ))
                .build();
    }
    
    @Override
    public ToolDefinition getDefinition() {
        return definition;
    }
    
    @Override
    public ToolResponse execute(Map<String, Object> parameters) {
        // Validate required parameters
        if (!parameters.containsKey("seller_id")) {
            return ToolResponse.error(getName(), "seller_id is a required parameter");
        }
        
        // Extract parameters
        String sellerId = parameters.get("seller_id").toString();
        
        // Handle time parameters
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        LocalDateTime startTime = LocalDateTime.parse("2025/02/01 00:00:00", DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
        LocalDateTime endTime = LocalDateTime.parse("2025/05/01 23:59:59", DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
        
        if (parameters.containsKey("start_time") && parameters.get("start_time") != null) {
            try {
                startTime = LocalDateTime.parse(parameters.get("start_time").toString() + " 00:00:00", 
                    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
            } catch (Exception e) {
                return ToolResponse.error(getName(), "Invalid start_time format, please use yyyy/MM/dd format");
            }
        }
        
        if (parameters.containsKey("end_time") && parameters.get("end_time") != null) {
            try {
                endTime = LocalDateTime.parse(parameters.get("end_time").toString() + " 23:59:59", 
                    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
            } catch (Exception e) {
                return ToolResponse.error(getName(), "Invalid end_time format, please use yyyy/MM/dd format");
            }
        }
        
        // Handle pagination parameters
        int page = 0;
        if (parameters.containsKey("page")) {
            if (parameters.get("page") instanceof Integer) {
                page = (Integer) parameters.get("page");
            } else {
                try {
                    page = Integer.parseInt(parameters.get("page").toString());
                } catch (NumberFormatException e) {
                    return ToolResponse.error(getName(), "page must be a valid integer");
                }
            }
        }
        
        int size = 20;
        if (parameters.containsKey("size")) {
            if (parameters.get("size") instanceof Integer) {
                size = (Integer) parameters.get("size");
            } else {
                try {
                    size = Integer.parseInt(parameters.get("size").toString());
                } catch (NumberFormatException e) {
                    return ToolResponse.error(getName(), "size must be a valid integer");
                }
            }
            
            // Limit maximum records per page
            if (size > 100) {
                size = 100;
            }
        }
        
        try {
            // Call service method
            Page<Order> orderPage = orderService.getOrdersWithFilters(
                sellerId,
                null, // productId
                null, // category
                startTime,
                endTime,
                page,
                size
            );
            
            // Build response
            Map<String, Object> result = new HashMap<>();
            result.put("orders", convertOrders(orderPage.getContent()));
            result.put("current_page", orderPage.getNumber());
            result.put("total_items", orderPage.getTotalElements());
            result.put("total_pages", orderPage.getTotalPages());
            result.put("start_time", startTime.format(dateFormatter));
            result.put("end_time", endTime.format(dateFormatter));
            
            return ToolResponse.success(getName(), result);
        } catch (Exception e) {
            return ToolResponse.error(getName(), "Error occurred while executing order query: " + e.getMessage());
        }
    }

    private List<Map<String, Object>> convertOrders(List<Order> orders) {
        return orders.stream().map(order -> {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("order_id", order.getOrderId());
            orderMap.put("product_id", order.getProductId());
            orderMap.put("buyer_id", order.getBuyerId());
            orderMap.put("seller_id", order.getSellerId());
            orderMap.put("unit_price", order.getUnitPrice());
            orderMap.put("quantity", order.getQuantity());
            orderMap.put("total_price", order.getTotalPrice());
            orderMap.put("timestamp", order.getTimestamp().toString());
            return orderMap;
        }).collect(Collectors.toList());
    }
} 