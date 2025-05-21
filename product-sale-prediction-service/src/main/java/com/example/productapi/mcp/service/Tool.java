package com.example.productapi.mcp.service;

import com.example.productapi.mcp.model.ToolDefinition;
import com.example.productapi.mcp.model.ToolResponse;

import java.util.Map;

/**
 * Interface for all tools that can be executed through MCP
 */
public interface Tool {
    /**
     * Get the tool's definition including metadata and schema
     * @return Tool definition
     */
    ToolDefinition getDefinition();
    
    /**
     * Execute the tool with the provided parameters
     * @param parameters Input parameters for the tool
     * @return Tool execution response
     */
    ToolResponse execute(Map<String, Object> parameters);
    
    /**
     * Get the unique name of the tool
     * @return Tool name
     */
    default String getName() {
        return getDefinition().getName();
    }
} 