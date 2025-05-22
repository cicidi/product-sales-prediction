package com.example.productapi.mcp.tools;

import com.example.productapi.mcp.model.ToolDefinition;
import com.example.productapi.mcp.model.ToolResponse;
import com.example.productapi.mcp.service.Tool;
import com.example.productapi.model.Product;
import com.example.productapi.service.ProductService;
import com.example.productapi.service.SalesPredictionService;
import com.example.productapi.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

@Component
public class SalesPredictionTool implements Tool {
    
    private final SalesPredictionService predictionService;
    private final ProductService productService;
    private final ToolDefinition definition;
    
    @Autowired
    public SalesPredictionTool(SalesPredictionService predictionService,
                              ProductService productService) {
        this.predictionService = predictionService;
        this.productService = productService;
        
        // Build the tool definition based on the Swagger API
        this.definition = ToolDefinition.builder()
                .name("predict_sales")
                .displayName("Sales Prediction")
                .description("Predict sales volume and revenue for specific products in future time periods based on historical data, helping sellers make better inventory and marketing decisions")
                .parameters(Arrays.asList(
                    ToolDefinition.ParameterDefinition.builder()
                        .name("product_id")
                        .type("string")
                        .description("Product ID, required parameter, specifies the product for sales prediction")
                        .required(true)
                        .example("P123456")
                        .build(),
                    ToolDefinition.ParameterDefinition.builder()
                        .name("seller_id")
                        .type("string")
                        .description("Seller ID, required parameter, specifies the product owner")
                        .required(true)
                        .example("SELLER789")
                        .build(),
                    ToolDefinition.ParameterDefinition.builder()
                        .name("start_time")
                        .type("string")
                        .description("Prediction start time, format yyyy/MM (e.g., 2025/06), optional parameter, defaults to current time")
                        .required(false)
                        .example("2025/06")
                        .build(),
                    ToolDefinition.ParameterDefinition.builder()
                        .name("end_time")
                        .type("string")
                        .description("Prediction end time, format yyyy/MM (e.g., 2025/08), optional parameter, defaults to 3 months after start time")
                        .required(false)
                        .example("2025/08")
                        .build()
                ))
                .outputSchema(Map.of(
                    "product_id", "Product ID",
                    "seller_id", "Seller ID",
                    "predictions", "List of predictions",
                    "start_time", "Prediction start time",
                    "end_time", "Prediction end time",
                    "total_predicted_sales", "Total predicted sales volume",
                    "total_predicted_revenue", "Total predicted revenue"
                ))
                .build();
    }
    
    @Override
    public ToolDefinition getDefinition() {
        return definition;
    }
    
    @Override
    public ToolResponse execute(Map<String, Object> parameters) {
        // Extract and validate required parameters
        if (!parameters.containsKey("product_id")) {
            return ToolResponse.error(getName(), "product_id is required");
        }
        if (!parameters.containsKey("seller_id")) {
            return ToolResponse.error(getName(), "seller_id is required");
        }
        
        // Extract parameters
        String productId = parameters.get("product_id").toString();
        String sellerId = parameters.get("seller_id").toString();
        
        // Get product information
        Product product;
        try {
            product = productService.getProductById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        } catch (Exception e) {
            return ToolResponse.error(getName(), "Error getting product: " + e.getMessage());
        }
        
        // Parse times
        LocalDateTime startTime;
        LocalDateTime endTime;
        try {
            startTime = parameters.containsKey("start_time") ? 
                TimeUtils.parseYearMonth(parameters.get("start_time").toString()) :
                LocalDateTime.now();
                
            endTime = parameters.containsKey("end_time") ? 
                TimeUtils.parseYearMonth(parameters.get("end_time").toString()).plusMonths(1).minusNanos(1) :
                startTime.plusMonths(3);
        } catch (Exception e) {
            return ToolResponse.error(getName(), "Error parsing dates: " + e.getMessage());
        }
        
        try {
            // Make prediction using the unified method
            Map<String, Object> prediction = predictionService.predictProductSales(
                product, sellerId, startTime, endTime);
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("product_id", productId);
            response.put("seller_id", sellerId);
            response.put("predictions", prediction.get("predictions"));
            response.put("start_time", startTime.toString());
            response.put("end_time", endTime.toString());
            response.put("total_predicted_sales", prediction.get("total_sales"));
            response.put("total_predicted_revenue", prediction.get("total_revenue"));
            
            return ToolResponse.success(getName(), response);
        } catch (Exception e) {
            return ToolResponse.error(getName(), "Error executing prediction: " + e.getMessage());
        }
    }
} 