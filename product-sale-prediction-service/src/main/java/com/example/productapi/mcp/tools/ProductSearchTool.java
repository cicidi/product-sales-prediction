package com.example.productapi.mcp.tools;

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
                .name("searchProducts")
                .displayName("Search Products")
                .description("Search for products by keyword and/or category")
                .parameters(Arrays.asList(
                    ToolDefinition.ParameterDefinition.builder()
                        .name("keyword")
                        .type("string")
                        .description("Search term to match against product name or description")
                        .required(false)
                        .example("laptop")
                        .build(),
                    ToolDefinition.ParameterDefinition.builder()
                        .name("category")
                        .type("string")
                        .description("Filter results by category")
                        .required(false)
                        .example("Electronics")
                        .build()
                ))
                .outputSchema(Map.of(
                    "results", "array of product objects",
                    "count", "number of products found"
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
        String keyword = parameters.containsKey("keyword") ? parameters.get("keyword").toString() : "";
        String category = parameters.containsKey("category") ? parameters.get("category").toString() : "";
        
        // Get all products and filter them
        List<Product> filteredProducts = productService.getAllProducts(keyword,category);

        // Build response
        Map<String, Object> result = new HashMap<>();
        result.put("results", filteredProducts);
        result.put("count", filteredProducts.size());
        result.put("query", Map.of(
            "keyword", keyword,
            "category", category
        ));
        
        return ToolResponse.success(getName(), result);
    }
} 