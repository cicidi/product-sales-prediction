package com.example.productapi.config;

import com.example.productapi.mcp.model.ToolDefinition;
import com.example.productapi.mcp.service.Tool;
import com.example.productapi.mcp.service.ToolRegistry;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class MCPOpenApiCustomizer implements OpenApiCustomizer {

    private final ToolRegistry toolRegistry;
    
    // Define the desired tool order and default data
    private static final List<Map<String, String>> DEFAULT_TOOLS = Arrays.asList(
        createDefaultTool("Sales Analytics", "analyze_sales", "analyze_sales", 
            "Analyze historical sales data to provide insights and trends"),
        createDefaultTool("Product Search", "search_products", "search_products",
            "Search products by keywords and optional filters"),
        createDefaultTool("Sales Prediction", "predict_product_sales", "predict_product_sales",
            "Predict future sales for a specific product"),
        createDefaultTool("Product Detail", "get_product_detail", "get_product_detail",
            "Get detailed information about a specific product"),
        createDefaultTool("Order List Query", "get_sellers_orders", "list_orders",
            "Query order records for a specific seller, supporting time range and pagination"),
        createDefaultTool("Product List", "list_products", "list_products",
            "List products with optional filtering by category and seller ID"),
        createDefaultTool("Similar Product Search", "search_similar_products", "search_similar_products",
            "Find similar products based on product ID or description"),
        createDefaultTool("Product Management", "manage_product", "manage_product",
            "Create, update, or delete product information")
    );

    private static final List<String> TOOL_ORDER = Arrays.asList(
        "analyze_sales",
        "search_products",
        "predict_product_sales",
        "get_product_detail",
        "get_sellers_orders",
        "list_products",
        "search_similar_products",
        "manage_product"
    );

    public MCPOpenApiCustomizer(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @Override
    public void customise(OpenAPI openApi) {
        if (openApi.getPaths() == null) {
            return;
        }
        
        openApi.getPaths().forEach((path, pathItem) -> {
            if (path.startsWith("/api/mcp")) {
                pathItem.readOperationsMap().forEach((httpMethod, operation) -> {
                    if ("listTools".equals(operation.getOperationId())) {
                        customizeListToolsResponse(operation);
                    } else if ("executeTool".equals(operation.getOperationId())) {
                        customizeExecuteToolRequest(operation);
                    }
                });
            }
        });
    }

    private void customizeExecuteToolRequest(Operation operation) {
        // Ensure request body exists
        if (operation.getRequestBody() == null) {
            operation.setRequestBody(new RequestBody());
        }
        
        RequestBody requestBody = operation.getRequestBody();
        if (requestBody.getContent() == null) {
            requestBody.setContent(new Content());
        }
        
        MediaType mediaType = requestBody.getContent().getOrDefault("application/json", new MediaType());
        
        // Add examples for each tool type
        
        // Example 1: Sales Analytics Tool
        Example salesAnalyticsExample = new Example();
        salesAnalyticsExample.setValue(Map.of(
            "toolName", "analyze_sales",
            "parameters", Map.of(
                "seller_id", "SELLER789",
                "start_time", "2025/01/01",
                "end_time", "2025/03/31",
                "top_n", 5
            )
        ));
        salesAnalyticsExample.setDescription("Execute Sales Analytics Tool");
        mediaType.addExamples("sales_analytics", salesAnalyticsExample);
        
        // Example 2: Product Detail Tool
        Example productDetailExample = new Example();
        productDetailExample.setValue(Map.of(
            "toolName", "get_product_detail",
            "parameters", Map.of(
                "product_id", "P123456"
            )
        ));
        productDetailExample.setDescription("Execute Product Detail Tool");
        mediaType.addExamples("product_detail", productDetailExample);
        
        // Example 3: Order List Tool
        Example orderListExample = new Example();
        orderListExample.setValue(Map.of(
            "toolName", "get_sellers_orders",
            "parameters", Map.of(
                "seller_id", "SELLER789",
                "start_time", "2025/02/01",
                "end_time", "2025/05/01",
                "page", 0,
                "size", 20
            )
        ));
        orderListExample.setDescription("Execute Order List Tool");
        mediaType.addExamples("order_list", orderListExample);

        // Update request body
        requestBody.getContent().addMediaType("application/json", mediaType);
        operation.setRequestBody(requestBody);
    }

    private void customizeListToolsResponse(Operation operation) {
        // Create example response using default tools
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", DEFAULT_TOOLS);
        response.put("error", null);
        response.put("toolName", "list_tools");

        // Create and configure the example
        Example example = new Example();
        example.setValue(response);
        example.setDescription("List of available MCP tools");
        example.setSummary("Default MCP tools list");

        // Ensure operation has responses
        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }

        // Configure 200 response
        ApiResponse apiResponse = operation.getResponses().getOrDefault("200", new ApiResponse());
        if (apiResponse.getContent() == null) {
            apiResponse.setContent(new Content());
        }

        // Configure media type
        MediaType mediaType = apiResponse.getContent().getOrDefault("application/json", new MediaType());
        mediaType.addExamples("default", example);

        // Add schema if needed
        if (mediaType.getSchema() == null) {
            mediaType.setSchema(new Schema<>().type("object"));
        }

        // Update the response configuration
        apiResponse.getContent().addMediaType("application/json", mediaType);
        operation.getResponses().addApiResponse("200", apiResponse);
    }

    private static Map<String, String> createDefaultTool(String displayName, String name, String operationId, String description) {
        Map<String, String> tool = new HashMap<>();
        tool.put("displayName", displayName);
        tool.put("name", name);
        tool.put("operationId", operationId);
        tool.put("description", description);
        return tool;
    }
} 