package com.example.productapi.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Defines the metadata and schema for a tool that can be invoked by LLMs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolDefinition {
    /**
     * Unique identifier for the tool
     */
    private String name;
    
    /**
     * Human-readable name
     */
    private String displayName;
    
    /**
     * Description of what the tool does
     */
    private String description;
    
    /**
     * Parameter definitions
     */
    private List<ParameterDefinition> parameters;
    
    /**
     * Output schema description
     */
    private Map<String, Object> outputSchema;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParameterDefinition {
        private String name;
        private String type;
        private String description;
        private boolean required;
        private Object defaultValue;
        private Object example;
    }
} 