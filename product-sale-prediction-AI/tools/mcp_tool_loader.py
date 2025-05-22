"""
MCP Tool Loader

This module handles the dynamic loading of Model-Centric Protocol (MCP) tools from a Swagger/OpenAPI specification.
It automatically converts API endpoints into LangChain tools that can be used by the agent.
"""

import json
import re
from typing import List, Dict, Any, Optional
import httpx
from langchain.tools import Tool
from pydantic import BaseModel, Field
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def sanitize_tool_name(name: str) -> str:
    """
    Sanitize tool name to match OpenAI's requirements.
    Only allows letters, numbers, underscores, and hyphens.
    """
    # Replace spaces and other characters with underscores
    name = re.sub(r'[^a-zA-Z0-9_-]', '_', name)
    # Remove consecutive underscores
    name = re.sub(r'_+', '_', name)
    # Remove leading/trailing underscores
    name = name.strip('_')
    return name

class MCPToolParameter(BaseModel):
    """Represents a parameter for an MCP tool."""
    name: str
    description: str
    required: bool = False
    type: str
    schema: Dict[str, Any] = Field(default_factory=dict)

class MCPTool(BaseModel):
    """Represents an MCP tool with its configuration."""
    operation_id: str
    name: str
    description: str
    method: str
    path: str
    parameters: List[MCPToolParameter] = Field(default_factory=list)
    request_body: Optional[Dict[str, Any]] = None

class SwaggerLoader:
    """Handles loading and parsing of Swagger/OpenAPI specifications."""
    
    def __init__(self, base_url: str):
        self.base_url = base_url
        self.client = httpx.Client(base_url=base_url, timeout=10.0)
    
    def check_server_status(self) -> bool:
        """Check if the MCP server is available."""
        try:
            response = self.client.get("/api/mcp/status")
            response.raise_for_status()
            return True
        except Exception as e:
            logger.error(f"Server status check failed: {str(e)}")
            return False
    
    def load_swagger_spec(self) -> Dict[str, Any]:
        """Load the Swagger/OpenAPI specification from the server."""
        try:
            response = self.client.get("/api-docs")
            response.raise_for_status()
            return response.json()
        except Exception as e:
            logger.error(f"Failed to load Swagger spec: {str(e)}")
            raise

    def extract_mcp_tools(self, swagger_spec: Dict[str, Any]) -> List[MCPTool]:
        """Extract MCP tools from the Swagger specification."""
        mcp_tools = []
        
        for path, path_item in swagger_spec.get("paths", {}).items():
            for method, operation in path_item.items():
                if "Model-Centric Protocol" not in operation.get("tags", []):
                    continue
                
                try:
                    # Extract operation details
                    operation_id = operation.get("operationId", "")
                    summary = operation.get("summary", "")
                    description = operation.get("description", summary)
                    
                    # Create a valid tool name
                    tool_name = sanitize_tool_name(summary or path.split("/")[-1])
                    
                    parameters = []
                    # Extract path and query parameters
                    for param in operation.get("parameters", []):
                        parameters.append(MCPToolParameter(
                            name=param["name"],
                            description=param.get("description", ""),
                            required=param.get("required", False),
                            type=param["schema"].get("type", "string"),
                            schema=param.get("schema", {})
                        ))
                    
                    # Extract request body if present
                    request_body = None
                    if "requestBody" in operation:
                        request_body = (operation["requestBody"]
                                      .get("content", {})
                                      .get("application/json", {})
                                      .get("schema", {}))
                    
                    tool = MCPTool(
                        operation_id=operation_id,
                        name=tool_name,
                        description=description,
                        method=method.upper(),
                        path=path,
                        parameters=parameters,
                        request_body=request_body
                    )
                    mcp_tools.append(tool)
                    logger.info(f"Extracted MCP tool: {tool.name}")
                except Exception as e:
                    logger.error(f"Failed to extract tool for {path} {method}: {str(e)}")
                    continue
        
        return mcp_tools

class ToolCreator:
    """Creates LangChain tools from MCP tool definitions."""
    
    def __init__(self, base_url: str):
        self.base_url = base_url
    
    def create_tool(self, mcp_tool: MCPTool) -> Tool:
        """Create a LangChain Tool from an MCP tool definition."""
        
        def tool_func(**kwargs) -> Any:
            """Execute the tool function."""
            with httpx.Client(base_url=self.base_url) as client:
                # Prepare parameters
                path_params = {}
                query_params = {}
                body_params = {}
                
                # Handle path and query parameters
                for param in mcp_tool.parameters:
                    if param.name in kwargs:
                        if "{" + param.name + "}" in mcp_tool.path:
                            path_params[param.name] = kwargs[param.name]
                        else:
                            query_params[param.name] = kwargs[param.name]
                
                # Handle request body
                if mcp_tool.request_body:
                    body_params = {
                        k: v for k, v in kwargs.items() 
                        if k not in query_params and k not in path_params
                    }
                
                # Format the path with path parameters
                try:
                    formatted_path = mcp_tool.path.format(**path_params) if path_params else mcp_tool.path
                except KeyError as e:
                    return {"error": f"Missing required path parameter: {str(e)}"}
                
                try:
                    if mcp_tool.method == "GET":
                        response = client.get(formatted_path, params=query_params)
                    else:  # POST
                        response = client.post(formatted_path, params=query_params, json=body_params)
                    
                    response.raise_for_status()
                    return response.json()
                except Exception as e:
                    logger.error(f"API call failed for {mcp_tool.name}: {str(e)}")
                    return {"error": str(e)}
        
        # Create tool schema
        param_schema = {}
        required_params = []
        
        # Add parameters to schema
        for param in mcp_tool.parameters:
            param_schema[param.name] = {
                "type": param.type,
                "description": param.description
            }
            if param.required:
                required_params.append(param.name)
        
        # Add request body parameters to schema
        if mcp_tool.request_body:
            properties = mcp_tool.request_body.get("properties", {})
            required_body_params = mcp_tool.request_body.get("required", [])
            
            for prop_name, prop_schema in properties.items():
                param_schema[prop_name] = {
                    "type": prop_schema.get("type", "string"),
                    "description": prop_schema.get("description", f"Request body parameter: {prop_name}")
                }
                if prop_name in required_body_params:
                    required_params.append(prop_name)
        
        # Create tool description
        description = mcp_tool.description
        if required_params:
            description += f"\n\nRequired parameters: {', '.join(required_params)}"
        
        return Tool.from_function(
            func=tool_func,
            name=mcp_tool.name,
            description=description,
            args_schema=param_schema
        )

def load_mcp_tools(base_url: str) -> List[Tool]:
    """
    Main function to load all MCP tools from the API server.
    
    Args:
        base_url: The base URL of the API server
        
    Returns:
        A list of LangChain tools created from the MCP endpoints
    """
    try:
        # Initialize components
        swagger_loader = SwaggerLoader(base_url)
        tool_creator = ToolCreator(base_url)
        
        # Check server status
        if not swagger_loader.check_server_status():
            logger.error("Server is not available")
            return []
        
        # Load and parse Swagger spec
        swagger_spec = swagger_loader.load_swagger_spec()
        mcp_tools = swagger_loader.extract_mcp_tools(swagger_spec)
        
        # Convert MCP tools to LangChain tools
        tools = []
        for mcp_tool in mcp_tools:
            try:
                tool = tool_creator.create_tool(mcp_tool)
                tools.append(tool)
                logger.info(f"Created tool: {tool.name}")
            except Exception as e:
                logger.error(f"Failed to create tool {mcp_tool.name}: {str(e)}")
                continue
        
        return tools
    except Exception as e:
        logger.error(f"Error loading MCP tools: {str(e)}")
        return [] 