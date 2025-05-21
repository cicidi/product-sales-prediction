package com.example.productapi.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard response format for tool execution results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolResponse {
    /**
     * Tool execution status
     */
    private String status; // success, error
    
    /**
     * Response data from the tool execution
     */
    private Object data;
    
    /**
     * Error message if any
     */
    private String error;
    
    /**
     * Tool name that was executed
     */
    private String toolName;
    
    /**
     * Static factory methods for common responses
     */
    public static ToolResponse success(String toolName, Object data) {
        return ToolResponse.builder()
                .status("success")
                .data(data)
                .toolName(toolName)
                .build();
    }
    
    public static ToolResponse error(String toolName, String errorMessage) {
        return ToolResponse.builder()
                .status("error")
                .error(errorMessage)
                .toolName(toolName)
                .build();
    }
} 