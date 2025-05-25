package com.example.productapi.mcp.tools;

import com.example.productapi.dto.TopSellingProductResponse;
import com.example.productapi.mcp.model.ToolDefinition;
import com.example.productapi.mcp.model.ToolResponse;
import com.example.productapi.mcp.service.Tool;
import com.example.productapi.service.SalesAnalyticsService;
import com.example.productapi.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SalesAnalyticsTool implements Tool {

    private final SalesAnalyticsService salesAnalyticsService;
    private final ToolDefinition definition;

    @Autowired
    public SalesAnalyticsTool(SalesAnalyticsService salesAnalyticsService) {
        this.salesAnalyticsService = salesAnalyticsService;
        
        // Initialize tool definition
        this.definition = ToolDefinition.builder()
            .name("analyze_sales")
            .displayName("Sales & Analytics")
            .description("Analyze historical sales data to provide insights and trends")
            .operationId("analyze_sales")
            .parameters(Arrays.asList(
                ToolDefinition.ParameterDefinition.builder()
                    .name("seller_id")
                    .type("string")
                    .description("Seller ID to filter sales (optional)")
                    .required(false)
                    .example("SELLER789")
                    .build(),
                ToolDefinition.ParameterDefinition.builder()
                    .name("start_time")
                    .type("string")
                    .description("Start time (format: yyyy/MM/dd)")
                    .required(true)
                    .example("2025/02/01")
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
                    .description("Number of top products to return")
                    .required(false)
                    .defaultValue(3)
                    .example(10)
                    .build()
            ))
            .outputSchema(Map.of(
                "products", "List of top selling products with details and metrics",
                "total_count", "Total number of products analyzed",
                "start_time", "Start time of analysis period",
                "end_time", "End time of analysis period",
                "seller_id", "Seller ID if filtered",
                "category", "Category if filtered"
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
            int topN = 3;
            if (parameters.containsKey("top_n")) {
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

            // Get analytics
            List<TopSellingProductResponse> results = salesAnalyticsService.getTopSellingProducts(
                sellerId,
                category,
                startTime,
                endTime,
                topN,
                true // Always include revenue in MCP responses
            );

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("products", results);
            response.put("total_count", results.size());
            response.put("start_time", startTime);
            response.put("end_time", endTime);
            response.put("seller_id", sellerId);
            response.put("category", category);

            return ToolResponse.success(getName(), response);
        } catch (Exception e) {
            return ToolResponse.error(getName(), "Error executing analytics: " + e.getMessage());
        }
    }
} 