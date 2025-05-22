package com.example.productapi.mcp.controller;

import com.example.productapi.mcp.model.ToolRequest;
import com.example.productapi.mcp.model.ToolResponse;
import com.example.productapi.mcp.service.ToolRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/mcp")
@Tag(name = "Model-Centric Protocol", description = "Model-Centric Protocol API - Provides tool invocation interface for Large Language Models (LLMs), supporting sales analysis and prediction functions")
public class MCPController {

    private final ToolRegistry toolRegistry;

    @Autowired
    public MCPController(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @Operation(
        summary = "Get all available tools",
        description = "Returns information about all tools callable through MCP, including sales analysis tools, sales prediction tools, etc.",
        operationId = "listTools"
    )
    @GetMapping("/tools")
    public ResponseEntity<ToolResponse> listTools() {
        return ResponseEntity.ok(
            ToolResponse.success("list_tools", toolRegistry.getAllTools())
        );
    }

    @Operation(
        summary = "Get tool details",
        description = "Returns detailed information about a specific tool by its name, including its parameters and output schema, such as parameters needed for product sales prediction and return value structure",
        operationId = "getToolDetails"
    )
    @GetMapping("/tools/{toolName}")
    public ResponseEntity<ToolResponse> getToolDetails(@PathVariable String toolName) {
        return toolRegistry.getTool(toolName)
                .map(tool -> ResponseEntity.ok(ToolResponse.success("tool_details", tool)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Execute tool",
        description = "Invoke a specific tool with provided parameters. For example: call the sales prediction tool to predict future sales of specific products; call the sales analysis tool to get sales rankings",
        operationId = "executeTool"
    )
    @PostMapping("/execute")
    public ResponseEntity<ToolResponse> executeTool(@RequestBody ToolRequest request) {
        String toolName = request.getToolName();
        
        if (toolName == null || toolName.isEmpty()) {
            return ResponseEntity.badRequest().body(
                ToolResponse.error("execute_tool", "Tool name is required")
            );
        }
        
        return toolRegistry.executeTool(toolName, request.getParameters())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @Operation(
        summary = "Health check",
        description = "Verify if MCP service is available and return current service version and number of available tools",
        operationId = "getStatus"
    )
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
            "status", "online",
            "version", "1.0.0",
            "toolCount", toolRegistry.getAllTools().size()
        ));
    }
} 