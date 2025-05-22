package com.example.productapi.mcp.tools;

import com.example.productapi.dto.SimilarProductSearchRequest;
import com.example.productapi.mcp.model.ToolDefinition;
import com.example.productapi.mcp.model.ToolResponse;
import com.example.productapi.mcp.service.Tool;
import com.example.productapi.model.Product;
import com.example.productapi.service.ProductService;
import com.example.productapi.service.ProductSimilarityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SimilarProductSearchTool implements Tool {

  private final ProductSimilarityService similarityService;
  private final ToolDefinition definition;
  private final ProductService productService;

  @Autowired
  public SimilarProductSearchTool(ProductSimilarityService similarityService,
      ProductService productService) {
    this.similarityService = similarityService;
    this.productService = productService;

    // Build tool definition
    this.definition = ToolDefinition.builder()
        .name("search_similar_products")
        .displayName("Similar Product Search")
        .description("Find similar products based on text description or product ID using vector similarity search technology")
        .parameters(Arrays.asList(
            ToolDefinition.ParameterDefinition.builder()
                .name("description")
                .type("string")
                .description("Product description text, used to find products similar to this description")
                .required(false)
                .example("Wireless noise-canceling headphones with Bluetooth connectivity")
                .build(),
            ToolDefinition.ParameterDefinition.builder()
                .name("product_id")
                .type("string")
                .description("Product ID, used to find other products similar to this product")
                .required(false)
                .example("P123456")
                .build(),
            ToolDefinition.ParameterDefinition.builder()
                .name("limit")
                .type("integer")
                .description("Limit on the number of results returned")
                .required(false)
                .defaultValue(3)
                .example(3)
                .build()
        ))
        .outputSchema(Map.of(
            "similar_products", "List of similar products",
            "total_count", "Number of products returned",
            "query_type", "Query type (description or product_id)",
            "query_value", "Query value"
        ))
        .build();
  }

  @Override
  public ToolDefinition getDefinition() {
    return definition;
  }

  @Override
  public ToolResponse execute(Map<String, Object> parameters) {
    // Validate parameters
    if (!parameters.containsKey("description") && !parameters.containsKey("product_id")) {
      return ToolResponse.error(getName(), "Either description or product_id parameter must be provided");
    }

    // Extract parameters
    String description =
        parameters.containsKey("description") ? parameters.get("description").toString() : null;
    String productId =
        parameters.containsKey("product_id") ? parameters.get("product_id").toString() : null;

    // Handle limit parameter
    int limit = 3;
    if (parameters.containsKey("limit")) {
      if (parameters.get("limit") instanceof Integer) {
        limit = (Integer) parameters.get("limit");
      } else {
        try {
          limit = Integer.parseInt(parameters.get("limit").toString());
        } catch (NumberFormatException e) {
          return ToolResponse.error(getName(), "limit must be a valid integer");
        }
      }
    }

    try {
      // Call service method
      List<Product> similarProducts = productService.findSimilarProducts(
          new SimilarProductSearchRequest(productId, description));

      // Build response
      Map<String, Object> response = new HashMap<>();
      response.put("similar_products", similarProducts);
      response.put("total_count", similarProducts.size());
      response.put("query_type", productId != null ? "product_id" : "description");
      response.put("query_value", productId != null ? productId : description);

      return ToolResponse.success(getName(), response);
    } catch (Exception e) {
      return ToolResponse.error(getName(), "Error occurred while searching for similar products: " + e.getMessage());
    }
  }
} 