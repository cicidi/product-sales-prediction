package com.example.productapi.config;

import com.example.productapi.mcp.model.ToolDefinition;
import com.example.productapi.mcp.service.ToolRegistry;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MCPOpenApiCustomizer implements OpenApiCustomizer {

    private final ToolRegistry toolRegistry;

    public MCPOpenApiCustomizer(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @Override
    public void customise(OpenAPI openApi) {
        openApi.getPaths().forEach((path, pathItem) -> {
            if (path.startsWith("/api/mcp")) {
                pathItem.readOperationsMap().forEach((httpMethod, operation) -> {
                    customizeOperation(operation);
                });
            }
        });
    }

    private void customizeOperation(Operation operation) {
        if ("listTools".equals(operation.getOperationId())) {
            customizeListToolsResponse(operation);
        }
    }

    private void customizeListToolsResponse(Operation operation) {
        List<Map<String, Object>> toolList = toolRegistry.getAllTools().stream()
            .map(tool -> {
                ToolDefinition def = tool.getDefinition();
                Map<String, Object> toolInfo = new HashMap<>();
                toolInfo.put("displayName", def.getDisplayName());
                toolInfo.put("name", def.getName());
                toolInfo.put("operationId", def.getOperationId());
                toolInfo.put("description", def.getDescription());
                return toolInfo;
            })
            .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", toolList);
        response.put("error", null);
        response.put("toolName", "list_tools");

        Example example = new Example();
        example.setValue(response);
        example.setDescription("List of available MCP tools");

        operation.getResponses().forEach((responseCode, apiResponse) -> {
            if ("200".equals(responseCode)) {
                Content content = apiResponse.getContent();
                if (content != null) {
                    MediaType mediaType = content.get("application/json");
                    if (mediaType != null) {
                        mediaType.addExamples("default", example);
                    }
                }
            }
        });
    }
} 