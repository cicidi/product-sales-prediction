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

    // 构建工具定义
    this.definition = ToolDefinition.builder()
        .name("search_similar_products")
        .displayName("相似商品搜索")
        .description("基于文本描述或商品ID查找相似的商品，使用向量相似度检索技术")
        .parameters(Arrays.asList(
            ToolDefinition.ParameterDefinition.builder()
                .name("description")
                .type("string")
                .description("商品描述文本，用于查找与此描述相似的商品")
                .required(false)
                .example("无线降噪耳机，支持蓝牙连接")
                .build(),
            ToolDefinition.ParameterDefinition.builder()
                .name("product_id")
                .type("string")
                .description("商品ID，用于查找与此商品相似的其他商品")
                .required(false)
                .example("P123456")
                .build(),
            ToolDefinition.ParameterDefinition.builder()
                .name("limit")
                .type("integer")
                .description("返回结果的数量限制")
                .required(false)
                .defaultValue(3)
                .example(3)
                .build()
        ))
        .outputSchema(Map.of(
            "similar_products", "相似商品列表",
            "total_count", "返回的商品数量",
            "query_type", "查询类型（description或product_id）",
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
    // 验证参数
    if (!parameters.containsKey("description") && !parameters.containsKey("product_id")) {
      return ToolResponse.error(getName(), "必须提供description或product_id参数之一");
    }

    // 提取参数
    String description =
        parameters.containsKey("description") ? parameters.get("description").toString() : null;
    String productId =
        parameters.containsKey("product_id") ? parameters.get("product_id").toString() : null;

    // 处理limit参数
    int limit = 3;
    if (parameters.containsKey("limit")) {
      if (parameters.get("limit") instanceof Integer) {
        limit = (Integer) parameters.get("limit");
      } else {
        try {
          limit = Integer.parseInt(parameters.get("limit").toString());
        } catch (NumberFormatException e) {
          return ToolResponse.error(getName(), "limit必须是有效的整数");
        }
      }
    }

    try {
      // 调用服务方法
      List<Product> similarProducts = productService.findSimilarProducts(
          new SimilarProductSearchRequest(productId, description));

      // 构建响应
      Map<String, Object> response = new HashMap<>();
      response.put("similar_products", similarProducts);
      response.put("total_count", similarProducts.size());
      response.put("query_type", productId != null ? "product_id" : "description");
      response.put("query_value", productId != null ? productId : description);

      return ToolResponse.success(getName(), response);
    } catch (Exception e) {
      return ToolResponse.error(getName(), "执行相似产品搜索时发生错误: " + e.getMessage());
    }
  }
} 