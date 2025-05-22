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
        .displayName("Search Products")
        .description("通过其他产品的Product Id 或者产品描述搜索类似的产品信息")
        .parameters(Arrays.asList(
            ToolDefinition.ParameterDefinition.builder()
                .name("product_id")
                .type("string")
                .description(
                    "其他产品的Product Id ，如果有就找类似的产品就先去数据库找产品信息，然后通过相关性找其他产品")
                .required(false)
                .example("P123456")
                .build(),
            ToolDefinition.ParameterDefinition.builder()
                .name("description")
                .type("string")
                .description("产品的特征描述，用来搜索类似的产品")
                .required(false)
                .example("iphone 16 pro max")
                .build(),
            ToolDefinition.ParameterDefinition.builder()
                .name("page")
                .type("integer")
                .description("页码，从0开始，可选参数，默认为0")
                .required(false)
                .defaultValue(0)
                .example(0)
                .build(),
            ToolDefinition.ParameterDefinition.builder()
                .name("size")
                .type("integer")
                .description("每页记录数，可选参数，默认为20，最大为100")
                .required(false)
                .defaultValue(20)
                .example(20)
                .build()
        ))
        .outputSchema(Map.of(
            "products", "搜索到的产品列表",
            "total_count", "产品总数",
            "current_page", "当前页码",
            "total_pages", "总页数",
            "query_type", "查询类型（product_id或description）",
            "query_value", "查询值"
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
                return ToolResponse.error(getName(), "page必须是有效的整数");
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
                return ToolResponse.error(getName(), "size必须是有效的整数");
            }
        }
        
        // 限制每页最大记录数
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