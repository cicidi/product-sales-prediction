package com.example.productapi.config;

import com.example.productapi.mcp.service.Tool;
import com.example.productapi.mcp.service.ToolRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import jakarta.annotation.PostConstruct;

import java.util.List;

@Configuration
public class MCPConfig {

    private final ToolRegistry toolRegistry;
    private final List<Tool> tools;

    @Autowired
    public MCPConfig(ToolRegistry toolRegistry, List<Tool> tools) {
        this.toolRegistry = toolRegistry;
        this.tools = tools;
    }

    @PostConstruct
    public void registerTools() {
        tools.forEach(toolRegistry::registerTool);
    }
} 