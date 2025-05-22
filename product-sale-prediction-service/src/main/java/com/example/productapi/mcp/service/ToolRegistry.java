package com.example.productapi.mcp.service;

import com.example.productapi.mcp.model.ToolResponse;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry service to manage and execute tools
 */
@Service
public class ToolRegistry {

  private final Map<String, Tool> tools = new ConcurrentHashMap<>();

  /**
   * Register a tool with the registry
   */
  public void registerTool(Tool tool) {
    tools.put(tool.getName(), tool);
  }

  /**
   * Get all registered tools
   */
  public List<Map<String, Object>> getAllTools() {
    return tools.values().stream()
        .map(tool -> {
          Map<String, Object> info = new HashMap<>();
          info.put("name", tool.getName());
          info.put("operationId", tool.getDefinition().getOperationId());
          info.put("displayName", tool.getDefinition().getDisplayName());
          info.put("description", tool.getDefinition().getDescription());
          return info;
        })
        .collect(Collectors.toList());
  }

  /**
   * Get a specific tool by name
   */
  public Optional<Tool> getTool(String name) {
    return Optional.ofNullable(tools.get(name));
  }

  /**
   * Execute a tool with the provided parameters
   */
  public Optional<ToolResponse> executeTool(String name, Map<String, Object> parameters) {
    return getTool(name)
        .map(tool -> {
          try {
            return tool.execute(parameters);
          } catch (Exception e) {
            return ToolResponse.error(name, "Error executing tool: " + e.getMessage());
          }
        });
  }
} 