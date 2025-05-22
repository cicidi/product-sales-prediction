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
public class ProductListTool implements Tool {
    
    private final ProductService productService;
    private final ToolDefinition definition;
    
    @Autowired
    public ProductListTool(ProductService productService) {
        this.productService = productService;
        
        // 构建工具定义 - 使用 snake_case
        this.definition = ToolDefinition.builder()
                .name("get_products")
                .displayName("产品列表查询")
                .description("获取产品列表，支持按类别和卖家ID筛选")
                .parameters(Arrays.asList(
                    ToolDefinition.ParameterDefinition.builder()
                        .name("category")
                        .type("string")
                        .description("商品类别，可选参数，用于按类别筛选产品")
                        .required(false)
                        .example("electronics")
                        .build(),
                    ToolDefinition.ParameterDefinition.builder()
                        .name("seller_id")
                        .type("string")
                        .description("卖家ID，可选参数，用于按卖家筛选产品")
                        .required(false)
                        .example("SELLER789")
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
                    "products", "产品列表",
                    "total_count", "产品总数",
                    "current_page", "当前页码",
                    "total_pages", "总页数",
                    "category", "查询的类别（如果提供）",
                    "seller_id", "查询的卖家ID（如果提供）"
                ))
                .build();
    }
    
    @Override
    public ToolDefinition getDefinition() {
        return definition;
    }
    
    @Override
    public ToolResponse execute(Map<String, Object> parameters) {
        // 提取参数
        String category = parameters.containsKey("category") ? parameters.get("category").toString() : null;
        String sellerId = parameters.containsKey("seller_id") ? parameters.get("seller_id").toString() : null;
        
        // 处理分页参数
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
            // 调用服务方法获取产品列表
            List<Product> products = productService.getAllProducts(category, sellerId);
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("products", products);
            response.put("total_count", products.size());
            response.put("current_page", page);
            response.put("total_pages", (int) Math.ceil(products.size() / (double) size));
            response.put("category", category);
            response.put("seller_id", sellerId);
            
            return ToolResponse.success(getName(), response);
        } catch (Exception e) {
            return ToolResponse.error(getName(), "获取产品列表时发生错误: " + e.getMessage());
        }
    }
} 