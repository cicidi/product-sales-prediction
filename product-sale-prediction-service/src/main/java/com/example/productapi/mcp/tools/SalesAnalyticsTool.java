package com.example.productapi.mcp.tools;

import com.example.productapi.dto.TopSellingProductResponse;
import com.example.productapi.dto.ProductSalesSummary;
import com.example.productapi.dto.SalesAnalyticsResponse;
import com.example.productapi.mcp.model.ToolDefinition;
import com.example.productapi.mcp.model.ToolResponse;
import com.example.productapi.mcp.service.Tool;
import com.example.productapi.service.SalesAnalyticsService;
import com.example.productapi.service.OrderService;
import com.example.productapi.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SalesAnalyticsTool implements Tool {

    private final SalesAnalyticsService salesAnalyticsService;
    private final OrderService orderService;
    private final ToolDefinition definition;

    @Autowired
    public SalesAnalyticsTool(SalesAnalyticsService salesAnalyticsService, OrderService orderService) {
        this.salesAnalyticsService = salesAnalyticsService;
        this.orderService = orderService;
        
        // Initialize tool definition
        this.definition = ToolDefinition.builder()
            .name("analyze_sales")
            .displayName("Sales Analytics")
            .description("Get daily product sales summary and total summary for a time range. If topN is provided, returns only top N products by total sales. For product details, use get_product_detail tool with the productId.")
            .operationId("analyze_sales")
            .parameters(Arrays.asList(
                ToolDefinition.ParameterDefinition.builder()
                    .name("seller_id")
                    .type("string")
                    .description("Seller ID to filter sales (optional)")
                    .required(false)
                    .example("seller_1")
                    .build(),
                ToolDefinition.ParameterDefinition.builder()
                    .name("product_id")
                    .type("string")
                    .description("Product ID to filter sales (optional)")
                    .required(false)
                    .example("p100")
                    .build(),
                ToolDefinition.ParameterDefinition.builder()
                    .name("start_time")
                    .type("string")
                    .description("Start time (format: yyyy/MM/dd)")
                    .required(true)
                    .example("2025/05/01")
                    .build(),
                ToolDefinition.ParameterDefinition.builder()
                    .name("end_time")
                    .type("string")
                    .description("End time (format: yyyy/MM/dd), defaults to current time")
                    .required(false)
                    .example("2025/05/01")
                    .build(),
                ToolDefinition.ParameterDefinition.builder()
                    .name("category")
                    .type("string")
                    .description("Category to filter by")
                    .required(false)
                    .example("electronics")
                    .build(),
                ToolDefinition.ParameterDefinition.builder()
                    .name("top_n")
                    .type("integer")
                    .description("Number of top products to return (optional, returns all if not provided)")
                    .required(false)
                    .example(10)
                    .build()
            ))
            .outputSchema(Map.of(
                "dailyProductSales", "List of daily product sales with productId, quantity, date, and revenue",
                "totalSummary", "List of total product sales summary with productId, quantity, date='total', and revenue",
                "startTime", "Start time of analysis period",
                "endTime", "End time of analysis period",
                "sellerId", "Seller ID if filtered",
                "productId", "Product ID if filtered",
                "category", "Category if filtered",
                "topN", "Top N filter if applied"
            ))
            .build();
    }

    @Override
    public ToolDefinition getDefinition() {
        return definition;
    }

    @Override
    public String getName() {
        return definition.getName();
    }

    @Override
    public ToolResponse execute(Map<String, Object> parameters) {
        try {
            // Extract parameters
            String sellerId = parameters.containsKey("seller_id") ? 
                parameters.get("seller_id").toString() : null;
            String productId = parameters.containsKey("product_id") ? 
                parameters.get("product_id").toString() : null;
            String category = parameters.containsKey("category") ? 
                parameters.get("category").toString() : null;

            // Parse start time
            if (!parameters.containsKey("start_time")) {
                return ToolResponse.error(getName(), "start_time parameter is required");
            }
            LocalDateTime startTime = TimeUtils.parseDate(parameters.get("start_time").toString());

            // Parse end time or use current time
            LocalDateTime endTime;
            if (parameters.containsKey("end_time") && parameters.get("end_time") != null) {
                endTime = TimeUtils.parseDateEndOfDay(parameters.get("end_time").toString());
            } else {
                endTime = LocalDateTime.now();
            }

            // Handle topN parameter
            Integer topN = null;
            if (parameters.containsKey("top_n") && parameters.get("top_n") != null) {
                if (parameters.get("top_n") instanceof Integer) {
                    topN = (Integer) parameters.get("top_n");
                } else {
                    try {
                        topN = Integer.parseInt(parameters.get("top_n").toString());
                    } catch (NumberFormatException e) {
                        return ToolResponse.error(getName(), "top_n must be a valid integer");
                    }
                }
            }

            // Get all orders for the time range
            List<com.example.productapi.model.Order> orders = orderService.getOrdersWithFilters(
                sellerId,
                productId,
                category,
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
            if (topN != null && topN > 0) {
                // Get top N products by total sales
                List<String> topProductIds = totalSummary.stream()
                    .limit(topN)
                    .map(ProductSalesSummary::getProductId)
                    .collect(Collectors.toList());
                
                // Filter daily sales to only include top N products
                dailyProductSales = dailyProductSales.stream()
                    .filter(summary -> topProductIds.contains(summary.getProductId()))
                    .collect(Collectors.toList());
                
                // Limit total summary to top N
                totalSummary = totalSummary.stream()
                    .limit(topN)
                    .collect(Collectors.toList());
            }

            // Build response
            SalesAnalyticsResponse.SalesAnalyticsResponseBuilder builder = SalesAnalyticsResponse.builder()
                .dailyProductSales(dailyProductSales)
                .totalSummary(totalSummary)
                .startTime(startTime)
                .endTime(endTime);
            
            // Add filter information to response
            if (sellerId != null) {
                builder.sellerId(sellerId);
            }
            if (productId != null) {
                builder.productId(productId);
            }
            if (category != null) {
                builder.category(category);
            }
            if (topN != null) {
                builder.topN(topN);
            }

            return ToolResponse.success(getName(), builder.build());
        } catch (Exception e) {
            return ToolResponse.error(getName(), "Error executing analytics: " + e.getMessage());
        }
    }
} 