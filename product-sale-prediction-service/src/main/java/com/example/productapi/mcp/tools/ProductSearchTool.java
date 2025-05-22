package com.example.productapi.mcp.tools;

import com.example.productapi.dto.SimilarProductSearchRequest;
import com.example.productapi.mcp.model.ToolDefinition;
import com.example.productapi.mcp.model.ToolResponse;
import com.example.productapi.mcp.service.Tool;
import com.example.productapi.model.Product;
import com.example.productapi.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ProductSearchTool implements Tool {

  private final ProductService productService;
  private final ToolDefinition definition;

  @Autowired
  public ProductSearchTool(ProductService productService) {
    this.productService = productService;

    // Build the tool definition
    this.definition = ToolDefinition.builder()
        .name("search_products")
        .displayName("Product Search")
        .description("Search products by keywords and optional filters")
        .operationId("search_products")
        .parameters(Arrays.asList(
            ToolDefinition.ParameterDefinition.builder()
                .name("product_id")
                .type("string")
                .description(
                    "Product ID of another product, if provided, will first find the product information in the database, then find other similar products based on relevance")
                .required(false)
                .example("P123456")
                .build(),
            ToolDefinition.ParameterDefinition.builder()
                .name("description")
                .type("string")
                .description("Product feature description, used to search for similar products")
                .required(false)
                .example("iphone 16 pro max")
                .build(),
            ToolDefinition.ParameterDefinition.builder()
                .name("page")
                .type("integer")
                .description("Page number, starting from 0, optional parameter, defaults to 0")
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
            "products", "List of found products",
            "total_count", "Total number of products",
            "current_page", "Current page number",
            "total_pages", "Total number of pages",
            "query_type", "Query type (product_id or description)",
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
    // Extract parameters with defaults
    String productId =
        parameters.containsKey("product_id") ? parameters.get("product_id").toString() : "";
    String description =
        parameters.containsKey("description") ? parameters.get("description").toString() : "";

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
        // Get filtered products
        List<Product> filteredProducts = productService.findSimilarProducts(
            new SimilarProductSearchRequest(productId, description));

        // Build response
        Map<String, Object> result = new HashMap<>();
        result.put("products", filteredProducts);
        result.put("total_count", filteredProducts.size());
        result.put("current_page", page);
        result.put("total_pages", (int) Math.ceil(filteredProducts.size() / (double) size));
        result.put("query_type", !productId.isEmpty() ? "product_id" : "description");
        result.put("query_value", !productId.isEmpty() ? productId : description);

        return ToolResponse.success(getName(), result);
    } catch (Exception e) {
        return ToolResponse.error(getName(), "Error searching products: " + e.getMessage());
    }
  }
} 