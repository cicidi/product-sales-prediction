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
        
        // Build tool definition
        this.definition = ToolDefinition.builder()
                .name("manage_product")
                .displayName("Product Management")
                .description("Create, update, or delete product information")
                .operationId("manage_product")
                .parameters(Arrays.asList(
                    ToolDefinition.ParameterDefinition.builder()
                        .name("product_id")
                        .type("string")
                        .description("Product ID, required for updates")
                        .required(false)
                        .example("P123456")
                        .build(),
                    ToolDefinition.ParameterDefinition.builder()
                        .name("seller_id")
                        .type("string")
                        .description("Seller ID, required for creation")
                        .required(false)
                        .example("SELLER789")
                        .build(),
                    ToolDefinition.ParameterDefinition.builder()
                        .name("name")
                        .type("string")
                        .description("Product name, required for creation")
                        .required(false)
                        .example("Wireless Bluetooth Earphones")
                        .build(),
                    ToolDefinition.ParameterDefinition.builder()
                        .name("category")
                        .type("string")
                        .description("Product category, required for creation")
                        .required(false)
                        .example("Electronics")
                        .build(),
                    ToolDefinition.ParameterDefinition.builder()
                        .name("brand")
                        .type("string")
                        .description("Brand, required for creation")
                        .required(false)
                        .example("Sony")
                        .build(),
                    ToolDefinition.ParameterDefinition.builder()
                        .name("price")
                        .type("number")
                        .description("Price, required for creation")
                        .required(false)
                        .example(999.99)
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
        // Determine if this is a create or update operation
        boolean isCreate = !parameters.containsKey("product_id");
        
        // Validate required fields for creation
        if (isCreate) {
            List<String> requiredFields = Arrays.asList("name", "category", "brand", "price", "seller_id");
            for (String field : requiredFields) {
                if (!parameters.containsKey(field)) {
                    return ToolResponse.error(getName(), "When creating a product, " + field + " is a required parameter");
                }
            }
        } else {
            // For updates, at least one update field is required
            if (parameters.size() <= 1) { // only id
                return ToolResponse.error(getName(), "When updating a product, at least one field must be provided for update");
            }
        }
        
        // Handle price field, ensure it's a number
        if (parameters.containsKey("price")) {
            Object priceObj = parameters.get("price");
            double price;
            
            if (priceObj instanceof Number) {
                price = ((Number) priceObj).doubleValue();
            } else {
                try {
                    price = Double.parseDouble(priceObj.toString());
                    parameters.put("price", price); // Update with parsed value
                } catch (NumberFormatException e) {
                    return ToolResponse.error(getName(), "price must be a valid number");
                }
            }
        }
        
        try {
            // Call service method
            Map<String, Object> result = new HashMap<>();
            
            if (isCreate) {
                // Create new product
                Object newProduct = productService.createProduct(parameters);
                result.put("product_id", newProduct);
                result.put("seller_id", parameters.get("seller_id"));
                result.put("name", parameters.get("name"));
                result.put("category", parameters.get("category"));
                result.put("brand", parameters.get("brand"));
                result.put("price", parameters.get("price"));
                result.put("created_at", new Date());
                result.put("updated_at", null);
                result.put("message", "Product created successfully");
            } else {
                // Update existing product
                String product_id = parameters.get("product_id").toString();
                Object updatedProduct = productService.updateProduct(product_id, parameters);
                
                if (updatedProduct == null) {
                    return ToolResponse.error(getName(), "Product with ID " + product_id + " not found");
                }
                
                result.put("product_id", product_id);
                result.put("seller_id", parameters.get("seller_id"));
                result.put("name", parameters.get("name"));
                result.put("category", parameters.get("category"));
                result.put("brand", parameters.get("brand"));
                result.put("price", parameters.get("price"));
                result.put("created_at", null);
                result.put("updated_at", new Date());
                result.put("message", "Product updated successfully");
            }
            
            return ToolResponse.success(getName(), result);
        } catch (Exception e) {
            return ToolResponse.error(getName(), "Error occurred while " + (isCreate ? "creating" : "updating") + " product: " + e.getMessage());
        }
    }
} 