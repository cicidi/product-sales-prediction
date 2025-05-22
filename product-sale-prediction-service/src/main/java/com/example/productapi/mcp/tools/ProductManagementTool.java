package com.example.productapi.mcp.tools;

import com.example.productapi.mcp.model.ToolDefinition;
import com.example.productapi.mcp.model.ToolResponse;
import com.example.productapi.mcp.service.Tool;
import com.example.productapi.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ProductManagementTool implements Tool {
    
    private final ProductService productService;
    private final ToolDefinition definition;
    
    @Autowired
    public ProductManagementTool(ProductService productService) {
        this.productService = productService;
        
        // 构建工具定义
        this.definition = ToolDefinition.builder()
                .name("manage_product")
                .displayName("产品管理")
                .description("创建新产品或更新现有产品信息")
                .parameters(Arrays.asList(
                    ToolDefinition.ParameterDefinition.builder()
                        .name("product_id")
                        .type("string")
                        .description("产品ID，更新时必填")
                        .required(false)
                        .example("P123456")
                        .build(),
                    ToolDefinition.ParameterDefinition.builder()
                        .name("seller_id")
                        .type("string")
                        .description("卖家ID，创建时必填")
                        .required(false)
                        .example("SELLER789")
                        .build(),
                    ToolDefinition.ParameterDefinition.builder()
                        .name("name")
                        .type("string")
                        .description("产品名称，创建时必填")
                        .required(false)
                        .example("无线蓝牙耳机")
                        .build(),
                    ToolDefinition.ParameterDefinition.builder()
                        .name("category")
                        .type("string")
                        .description("产品类别，创建时必填")
                        .required(false)
                        .example("电子产品")
                        .build(),
                    ToolDefinition.ParameterDefinition.builder()
                        .name("brand")
                        .type("string")
                        .description("品牌，创建时必填")
                        .required(false)
                        .example("Sony")
                        .build(),
                    ToolDefinition.ParameterDefinition.builder()
                        .name("price")
                        .type("number")
                        .description("价格，创建时必填")
                        .required(false)
                        .example(999.99)
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
        // 判断是创建还是更新操作
        boolean isCreate = !parameters.containsKey("product_id");
        
        // 创建操作的必填字段验证
        if (isCreate) {
            List<String> requiredFields = Arrays.asList("name", "category", "brand", "price", "seller_id");
            for (String field : requiredFields) {
                if (!parameters.containsKey(field)) {
                    return ToolResponse.error(getName(), "创建产品时，" + field + "是必填参数");
                }
            }
        } else {
            // 更新操作，至少需要一个更新字段
            if (parameters.size() <= 1) { // 只有id
                return ToolResponse.error(getName(), "更新产品时，需要至少提供一个要更新的字段");
            }
        }
        
        // 处理价格字段，确保为数字
        if (parameters.containsKey("price")) {
            Object priceObj = parameters.get("price");
            double price;
            
            if (priceObj instanceof Number) {
                price = ((Number) priceObj).doubleValue();
            } else {
                try {
                    price = Double.parseDouble(priceObj.toString());
                    parameters.put("price", price); // 更新为解析后的数值
                } catch (NumberFormatException e) {
                    return ToolResponse.error(getName(), "price必须是有效的数字");
                }
            }
        }
        
        try {
            // 调用服务方法
            Map<String, Object> result = new HashMap<>();
            
            if (isCreate) {
                // 创建新产品
                Object newProduct = productService.createProduct(parameters);
                result.put("product_id", newProduct);
                result.put("seller_id", parameters.get("seller_id"));
                result.put("name", parameters.get("name"));
                result.put("category", parameters.get("category"));
                result.put("brand", parameters.get("brand"));
                result.put("price", parameters.get("price"));
                result.put("created_at", new Date());
                result.put("updated_at", null);
                result.put("message", "产品创建成功");
            } else {
                // 更新现有产品
                String product_id = parameters.get("product_id").toString();
                Object updatedProduct = productService.updateProduct(product_id, parameters);
                
                if (updatedProduct == null) {
                    return ToolResponse.error(getName(), "未找到ID为 " + product_id + " 的产品");
                }
                
                result.put("product_id", product_id);
                result.put("seller_id", parameters.get("seller_id"));
                result.put("name", parameters.get("name"));
                result.put("category", parameters.get("category"));
                result.put("brand", parameters.get("brand"));
                result.put("price", parameters.get("price"));
                result.put("created_at", null);
                result.put("updated_at", new Date());
                result.put("message", "产品更新成功");
            }
            
            return ToolResponse.success(getName(), result);
        } catch (Exception e) {
            return ToolResponse.error(getName(), "执行产品" + (isCreate ? "创建" : "更新") + "操作时发生错误: " + e.getMessage());
        }
    }
} 