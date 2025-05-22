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

    // 构建工具定义 - 使用 snake_case
    this.definition = ToolDefinition.builder()
        .name("get_product_by_id")
        .displayName("产品详情查询")
        .description("根据产品ID获取产品详细信息")
        .parameters(Arrays.asList(
            ToolDefinition.ParameterDefinition.builder()
                .name("product_id")
                .type("string")
                .description("产品ID，必填参数，用于查询特定产品的详细信息")
                .required(true)
                .example("P123456")
                .build()
        ))
        .outputSchema(Map.of(
            "product_id", "产品ID",
            "seller_id", "卖家ID",
            "name", "产品名称",
            "category", "产品类别",
            "brand", "品牌",
            "price", "价格",
            "created_at", "创建时间",
            "updated_at", "更新时间"
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
      return ToolResponse.error(getName(), "product_id是必填参数");
    }

    String productId = parameters.get("product_id").toString();

    try {
      // 调用服务方法获取产品信息
      Product product = productService.getProductById(productId)
          .orElse(null);

      if (product == null) {
        return ToolResponse.error(getName(), "未找到ID为 " + productId + " 的产品");
      }

      return ToolResponse.success(getName(), product);
    } catch (Exception e) {
      return ToolResponse.error(getName(), "获取产品详情时发生错误: " + e.getMessage());
    }
  }
} 