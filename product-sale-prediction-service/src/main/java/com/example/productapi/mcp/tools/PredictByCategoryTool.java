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
import java.util.List;
import java.util.Map;

@Component
public class PredictByCategoryTool implements Tool {

  private final PredictionService predictionService;
  private final ToolDefinition definition;

  @Autowired
  public PredictByCategoryTool(PredictionService predictionService) {
    this.predictionService = predictionService;

    // Build the tool definition
    this.definition = ToolDefinition.builder()
        .name("predict_by_category")
        .displayName("Predict by Category")
        .description("Predict future Top/Best N sell products within a specific category.")
        .operationId("predict_top_N_sale_by_category")
        .parameters(Arrays.asList(
            ToolDefinition.ParameterDefinition.builder()
                .name("category")
                .type("string")
                .description(
                    "Category, required parameter, specifies the category for sales prediction")
                .required(true)
                .example("electronics")
                .build(),
            ToolDefinition.ParameterDefinition.builder()
                .name("seller_id")
                .type("string")
                .description(
                    "Seller ID, required parameter, specifies the seller for sales prediction")
                .required(true)
                .example("seller_1")
                .build(),
            ToolDefinition.ParameterDefinition.builder()
                .name("top_n")
                .type("integer")
                .description("Number of top products to predict (Required)")
                .required(true)
                .example(10)
                .build(),
            ToolDefinition.ParameterDefinition.builder()
                .name("start_date")
                .type("string")
                .description(
                    "Start date for prediction, format yyyy/MM/dd (e.g., 2025/06/01), required parameter")
                .required(true)
                .example("2025/06/01")
                .build(),
            ToolDefinition.ParameterDefinition.builder()
                .name("end_date")
                .type("string")
                .description(
                    "End date for prediction, format yyyy/MM/dd (e.g., 2025/06/01), optional parameter, if not provided will only predict one day")
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
    if (!parameters.containsKey("category")) {
      return ToolResponse.error(getName(), "category is required");
    }
    if (!parameters.containsKey("seller_id")) {
      return ToolResponse.error(getName(), "seller_id is required");
    }
    if (!parameters.containsKey("start_date")) {
      return ToolResponse.error(getName(), "start_date is required");
    }

    // Extract parameters
    String category = parameters.get("category").toString();
    String sellerId = parameters.get("seller_id").toString();

    // Parse topN (optional)
    Integer topN = null;
    if (parameters.containsKey("top_n")) {
      try {
        topN = Integer.parseInt(parameters.get("top_n").toString());
      } catch (NumberFormatException e) {
        return ToolResponse.error(getName(), "Invalid top_n format");
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
      return ToolResponse.error(getName(),
          "Error parsing dates (expected format yyyy/MM/dd): " + e.getMessage());
    }

    try {
      // Call predicateTopSales
      List<Predications> predictions = predictionService.predicateTopSales(sellerId, category,
          startDate, endDate, topN);

      // Build response
      Map<String, Object> response = new HashMap<>();
      response.put("category", category);
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