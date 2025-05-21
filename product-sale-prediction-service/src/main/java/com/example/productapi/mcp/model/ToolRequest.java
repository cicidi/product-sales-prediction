package com.example.productapi.mcp.model;

import lombok.Data;
import java.util.Map;

/**
 * Represents a standard request format for tool invocation from LLMs
 */
@Data
public class ToolRequest {
    /**
     * The name of the tool to invoke
     */
    private String toolName;
    
    /**
     * Parameters to pass to the tool
     */
    private Map<String, Object> parameters;
    
    /**
     * Optional metadata for tracking purposes
     */
    private Map<String, Object> metadata;
} 