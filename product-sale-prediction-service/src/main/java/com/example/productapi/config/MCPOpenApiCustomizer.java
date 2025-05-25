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
    
    // Define the order of tools
    private static final List<String> TOOL_ORDER = Arrays.asList(
        "list_orders",
        "list_products",
        "manage_product",
        "analyze_sales",
        "predict_product_sales",
        "get_product_detail",
        "search_products",
        "search_similar_products"
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
        
        // 获取实际的工具定义
        List<Tool> tools = toolRegistry.getAllTools().stream()
            .filter(t -> t instanceof Tool)
            .map(t -> (Tool) t)
            .collect(Collectors.toList());
        
        // 为每种工具类型添加示例
        for (Tool tool : tools) {
            ToolDefinition def = tool.getDefinition();
            String toolName = def.getName();
            
            // 创建示例参数
            Map<String, Object> sampleParams = new HashMap<>();
            
            // 为每个参数添加示例值
            if (def.getParameters() != null) {
                for (ToolDefinition.ParameterDefinition param : def.getParameters()) {
                    if (param.getExample() != null) {
                        sampleParams.put(param.getName(), param.getExample());
                    } else if (param.getDefaultValue() != null) {
                        sampleParams.put(param.getName(), param.getDefaultValue());
                    }
                }
            }
            
            // 创建工具执行示例
            Example toolExample = new Example();
            toolExample.setValue(Map.of(
                "toolName", toolName,
                "parameters", sampleParams
            ));
            toolExample.setDescription("Execute " + def.getDisplayName());
            mediaType.addExamples(toolName, toolExample);
        }

        // Update request body
        requestBody.getContent().addMediaType("application/json", mediaType);
        operation.setRequestBody(requestBody);
    }

    private void customizeListToolsResponse(Operation operation) {
        // 获取所有工具的信息
        List<Map<String, Object>> toolList = new ArrayList<>();
        
        // 创建工具映射以便按顺序查找
        Map<String, Tool> toolMap = toolRegistry.getAllTools().stream()
            .filter(t -> t instanceof Tool)
            .map(t -> (Tool) t)
            .collect(Collectors.toMap(
                t -> t.getName(),
                t -> t,
                (existing, replacement) -> existing
            ));
        
        // 按照定义的顺序添加工具信息
        for (String toolName : TOOL_ORDER) {
            if (toolMap.containsKey(toolName)) {
                Tool tool = toolMap.get(toolName);
                ToolDefinition def = tool.getDefinition();
                
                Map<String, Object> toolInfo = new HashMap<>();
                toolInfo.put("displayName", def.getDisplayName());
                toolInfo.put("name", def.getName());
                toolInfo.put("operationId", def.getOperationId());
                toolInfo.put("description", def.getDescription());
                
                toolList.add(toolInfo);
            }
        }
        
        // 添加不在顺序列表中的其他工具
        for (Tool tool : toolMap.values()) {
            String toolName = tool.getName();
            if (!TOOL_ORDER.contains(toolName)) {
                ToolDefinition def = tool.getDefinition();
                
                Map<String, Object> toolInfo = new HashMap<>();
                toolInfo.put("displayName", def.getDisplayName());
                toolInfo.put("name", def.getName());
                toolInfo.put("operationId", def.getOperationId());
                toolInfo.put("description", def.getDescription());
                
                toolList.add(toolInfo);
            }
        }

        // 创建响应示例
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", toolList);
        response.put("error", null);
        response.put("toolName", "list_tools");

        // 创建并配置示例
        Example example = new Example();
        example.setValue(response);
        example.setDescription("List of available MCP tools");
        example.setSummary("MCP tools list");

        // 确保操作有响应
        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }

        // 配置200响应
        ApiResponse apiResponse = operation.getResponses().getOrDefault("200", new ApiResponse());
        if (apiResponse.getContent() == null) {
            apiResponse.setContent(new Content());
        }

        // 配置媒体类型
        MediaType mediaType = apiResponse.getContent().getOrDefault("application/json", new MediaType());
        mediaType.addExamples("default", example);

        // 如果需要，添加schema
        if (mediaType.getSchema() == null) {
            mediaType.setSchema(new Schema<>().type("object"));
        }

        // 更新响应配置
        apiResponse.getContent().addMediaType("application/json", mediaType);
        operation.getResponses().addApiResponse("200", apiResponse);
    }
} 