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
        
        // Build tool definition - using snake_case
        this.definition = ToolDefinition.builder()
                .name("get_products")
                .displayName("Product List Query")
                .description("Get product list, supporting filtering by category and seller ID")
                .parameters(Arrays.asList(
                    ToolDefinition.ParameterDefinition.builder()
                        .name("category")
                        .type("string")
                        .description("Product category, optional parameter, used to filter products by category")
                        .required(false)
                        .example("electronics")
                        .build(),
                    ToolDefinition.ParameterDefinition.builder()
                        .name("seller_id")
                        .type("string")
                        .description("Seller ID, optional parameter, used to filter products by seller")
                        .required(false)
                        .example("SELLER789")
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
                    "products", "Product list",
                    "total_count", "Total number of products",
                    "current_page", "Current page number",
                    "total_pages", "Total number of pages",
                    "category", "Queried category (if provided)",
                    "seller_id", "Queried seller ID (if provided)"
                ))
                .build();
    }
    
    @Override
    public ToolDefinition getDefinition() {
        return definition;
    }
    
    @Override
    public ToolResponse execute(Map<String, Object> parameters) {
        // Extract parameters
        String category = parameters.containsKey("category") ? parameters.get("category").toString() : null;
        String sellerId = parameters.containsKey("seller_id") ? parameters.get("seller_id").toString() : null;
        
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
            // Call service method to get product list
            List<Product> products = productService.getAllProducts(category, sellerId);
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("products", products);
            response.put("total_count", products.size());
            response.put("current_page", page);
            response.put("total_pages", (int) Math.ceil(products.size() / (double) size));
            response.put("category", category);
            response.put("seller_id", sellerId);
            
            return ToolResponse.success(getName(), response);
        } catch (Exception e) {
            return ToolResponse.error(getName(), "Error occurred while getting product list: " + e.getMessage());
        }
    }
} 