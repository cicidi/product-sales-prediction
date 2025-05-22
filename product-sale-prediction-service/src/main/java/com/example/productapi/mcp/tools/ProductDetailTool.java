package com.example.productapi.mcp.tools;

import com.example.productapi.mcp.model.ToolDefinition;
import com.example.productapi.mcp.model.ToolResponse;
import com.example.productapi.mcp.service.Tool;
import com.example.productapi.model.Product;
import com.example.productapi.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ProductDetailTool implements Tool {

  private final ProductService productService;
  private final ToolDefinition definition;

  @Autowired
  public ProductDetailTool(ProductService productService) {
    this.productService = productService;

    // Build tool definition - using snake_case
    this.definition = ToolDefinition.builder()
        .name("get_product_by_id")
        .displayName("Product Detail Query")
        .description("Get detailed product information by product ID")
        .parameters(Arrays.asList(
            ToolDefinition.ParameterDefinition.builder()
                .name("product_id")
                .type("string")
                .description("Product ID, required parameter, used to query specific product details")
                .required(true)
                .example("P123456")
                .build()
        ))
        .outputSchema(Map.of(
            "product_id", "Product ID",
            "seller_id", "Seller ID",
            "name", "Product name",
            "category", "Product category",
            "brand", "Brand",
            "price", "Price",
            "created_at", "Creation time",
            "updated_at", "Update time"
        ))
        .build();
  }

  @Override
  public ToolDefinition getDefinition() {
    return definition;
  }

  @Override
  public ToolResponse execute(Map<String, Object> parameters) {
    if (!parameters.containsKey("product_id")) {
      return ToolResponse.error(getName(), "product_id is a required parameter");
    }

    String productId = parameters.get("product_id").toString();

    try {
      // Call service method to get product information
      Product product = productService.getProductById(productId)
          .orElse(null);

      if (product == null) {
        return ToolResponse.error(getName(), "Product with ID " + productId + " not found");
      }

      return ToolResponse.success(getName(), product);
    } catch (Exception e) {
      return ToolResponse.error(getName(), "Error occurred while getting product details: " + e.getMessage());
    }
  }
} 