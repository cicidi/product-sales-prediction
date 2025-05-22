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
                .displayName("销量预测")
                .description("基于历史数据预测特定商品在未来时间段的销量和销售额，帮助卖家做出更好的库存和营销决策")
                .parameters(Arrays.asList(
                    ToolDefinition.ParameterDefinition.builder()
                        .name("product_id")
                        .type("string")
                        .description("商品ID，必填参数，指定需要预测销量的商品")
                        .required(true)
                        .example("P123456")
                        .build(),
                    ToolDefinition.ParameterDefinition.builder()
                        .name("seller_id")
                        .type("string")
                        .description("卖家ID，必填参数，指定商品所属卖家")
                        .required(true)
                        .example("SELLER789")
                        .build(),
                    ToolDefinition.ParameterDefinition.builder()
                        .name("start_time")
                        .type("string")
                        .description("预测开始时间，格式为yyyy/MM（如：2025/06），可选参数，默认为当前时间")
                        .required(false)
                        .example("2025/06")
                        .build(),
                    ToolDefinition.ParameterDefinition.builder()
                        .name("end_time")
                        .type("string")
                        .description("预测结束时间，格式为yyyy/MM（如：2025/08），可选参数，默认为开始时间后3个月")
                        .required(false)
                        .example("2025/08")
                        .build()
                ))
                .outputSchema(Map.of(
                    "product_id", "商品ID",
                    "seller_id", "卖家ID",
                    "predictions", "预测结果列表",
                    "start_time", "预测开始时间",
                    "end_time", "预测结束时间",
                    "total_predicted_sales", "总预测销量",
                    "total_predicted_revenue", "总预测销售额"
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