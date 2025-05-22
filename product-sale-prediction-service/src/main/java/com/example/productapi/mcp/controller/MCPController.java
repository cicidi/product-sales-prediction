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
@Tag(name = "Model-Centric Protocol", description = "模型中心协议API - 为大语言模型(LLM)提供工具调用接口，支持销售分析和预测功能")
public class MCPController {

    private final ToolRegistry toolRegistry;

    @Autowired
    public MCPController(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @Operation(
        summary = "获取所有可用工具",
        description = "返回所有通过MCP可调用的工具信息，包括销售分析工具、销售预测工具等"
    )
    @GetMapping("/tools")
    public ResponseEntity<ToolResponse> listTools() {
        return ResponseEntity.ok(
            ToolResponse.success("list_tools", toolRegistry.getAllTools())
        );
    }

    @Operation(
        summary = "获取工具详细信息",
        description = "根据工具名称返回特定工具的详细信息，包括其参数和输出模式，如商品销售预测需要的参数和返回值结构"
    )
    @GetMapping("/tools/{toolName}")
    public ResponseEntity<ToolResponse> getToolDetails(@PathVariable String toolName) {
        return toolRegistry.getTool(toolName)
                .map(tool -> ResponseEntity.ok(ToolResponse.success("tool_details", tool)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "执行工具",
        description = "使用提供的参数调用特定工具。例如：调用销售预测工具预测特定商品未来销量；调用销售分析工具获取销售排行"
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
        summary = "健康检查",
        description = "验证MCP服务是否可用，并返回当前服务版本和可用工具数量"
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