package com.example.productapi.mcp.tools;

import com.example.productapi.mcp.model.ToolDefinition;
import com.example.productapi.mcp.model.ToolResponse;
import com.example.productapi.mcp.service.Tool;
import com.example.productapi.model.Predications;
import com.example.productapi.service.PredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
public class PredictByProductIdTool implements Tool {

    private final PredictionService predictionService;
    private final ToolDefinition definition;

    @Autowired
    public PredictByProductIdTool(PredictionService predictionService) {
        this.predictionService = predictionService;

        // Build the tool definition
        this.definition = ToolDefinition.builder()
                .name("predict_by_product_id")
                .displayName("Predict by Product ID")
                .description("Predict future sales for a specific product by product ID.")
                .operationId("predict_by_product_id")
                .parameters(Arrays.asList(
                    ToolDefinition.ParameterDefinition.builder()
                        .name("product_id")
                        .type("string")
                        .description("Product ID, required parameter, specifies the product for sales prediction")
                        .required(true)
                        .example("p100")
                        .build(),
                    ToolDefinition.ParameterDefinition.builder()
                        .name("seller_id")
                        .type("string")
                        .description("Seller ID, required parameter, specifies the seller for sales prediction")
                        .required(true)
                        .example("seller_1")
                        .build(),
                    ToolDefinition.ParameterDefinition.builder()
                        .name("sale_price")
                        .type("number")
                        .description("Sale price (optional, if not provided will use original price)")
                        .required(false)
                        .example(99.99)
                        .build(),
                    ToolDefinition.ParameterDefinition.builder()
                        .name("start_date")
                        .type("string")
                        .description("Start date for prediction, format yyyy/MM/dd (e.g., 2025/06/01), required parameter")
                        .required(true)
                        .example("2025/06/01")
                        .build(),
                    ToolDefinition.ParameterDefinition.builder()
                        .name("end_date")
                        .type("string")
                        .description("End date for prediction, format yyyy/MM/dd (e.g., 2025/06/01), optional parameter, if not provided will only predict one day")
                        .required(false)
                        .example("2025/06/01")
                        .build()
                ))
                .outputSchema(Map.of(
                    "predicationList", "List of daily predictions",
                    "startDate", "Prediction start date",
                    "endDate", "Prediction end date", 
                    "totalQuantity", "Total predicted sales quantity",
                    "totalDays", "Total number of days predicted"
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
        if (!parameters.containsKey("start_date")) {
            return ToolResponse.error(getName(), "start_date is required");
        }

        // Extract parameters
        String productId = parameters.get("product_id").toString();
        String sellerId = parameters.get("seller_id").toString();

        // Parse sale price (optional)
        Double salePrice = null;
        if (parameters.containsKey("sale_price")) {
            try {
                salePrice = Double.parseDouble(parameters.get("sale_price").toString());
            } catch (NumberFormatException e) {
                return ToolResponse.error(getName(), "Invalid sale_price format");
            }
        }

        // Parse dates
        LocalDate startDate;
        LocalDate endDate = null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        try {
            startDate = LocalDate.parse(parameters.get("start_date").toString(), formatter);

            if (parameters.containsKey("end_date")) {
                endDate = LocalDate.parse(parameters.get("end_date").toString(), formatter);
            }
        } catch (Exception e) {
            return ToolResponse.error(getName(), "Error parsing dates (expected format yyyy/MM/dd): " + e.getMessage());
        }

        try {
            // Call predictSalesByProductId
            Predications predictions = predictionService.predictSalesByProductId(productId, sellerId, salePrice, startDate, endDate);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("product_id", productId);
            response.put("seller_id", sellerId);
            response.put("predictions", predictions);
            response.put("start_date", startDate.toString());
            response.put("end_date", endDate != null ? endDate.toString() : null);

            return ToolResponse.success(getName(), response);
        } catch (Exception e) {
            return ToolResponse.error(getName(), "Error executing prediction: " + e.getMessage());
        }
    }
} 