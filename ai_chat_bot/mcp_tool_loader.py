import logging
import json
from typing import List, Dict, Any, Optional, Callable, Union, Type, TypedDict, get_type_hints, get_origin, get_args

import httpx
from pydantic import BaseModel, create_model, Field
from langchain.tools import Tool, StructuredTool

def get_tool_details(tool_name: str) -> Dict[str, Any]:
    """
    Fetch detailed information about a specific tool
    
    Args:
        tool_name: Name of the tool to fetch details for
        
    Returns:
        Dictionary containing detailed tool information
    """
    try:
        url = f"http://localhost:8080/api/mcp/tools/{tool_name}"
        response = httpx.get(url, timeout=10)
        
        if response.status_code != 200:
            logging.error(f"Failed to fetch tool details for {tool_name}: {response.status_code} - {response.text}")
            return {}
            
        response_data = response.json()
        if response_data.get("status") != "success" or "data" not in response_data:
            logging.error(f"Invalid tool details response for {tool_name}: {response_data}")
            return {}
            
        return response_data.get("data", {})
    except Exception as e:
        logging.error(f"Error fetching tool details for {tool_name}: {str(e)}")
        return {}

def get_python_type(param_type: str) -> Type:
    """Convert MCP parameter type to Python type"""
    type_mapping = {
        "string": str,
        "integer": int,
        "number": float,
        "boolean": bool,
        "array": list,
        "object": dict
    }
    return type_mapping.get(param_type, str)

def build_mcp_tool(tool_data: Dict[str, Any], tool_details: Dict[str, Any]) -> Optional[StructuredTool]:
    """
    Build a StructuredTool from MCP tool data with parameter details
    
    Args:
        tool_data: Basic tool metadata from MCP API
        tool_details: Detailed tool information including parameters
        
    Returns:
        A langchain StructuredTool configured to call the MCP execute endpoint with parameter validation
    """
    try:
        name = tool_data.get("name", "")
        operation_id = tool_data.get("operationId", "")
        display_name = tool_data.get("displayName", "")
        description = tool_data.get("description", "")
        
        # Extract parameter information from tool details
        parameters = []
        required_params = []
        param_fields = {}
        
        definition = tool_details.get("definition", {})
        if definition:
            parameters = definition.get("parameters", [])
            
            for param in parameters:
                param_name = param.get("name")
                if not param_name:
                    continue
                
                param_type = param.get("type", "string")
                python_type = get_python_type(param_type)
                param_desc = param.get("description", "")
                param_example = param.get("example")
                param_required = param.get("required", False)
                param_default = param.get("defaultValue")
                
                if param_required:
                    required_params.append(param_name)
                
                # Define field for the parameter
                if param_required:
                    # Required parameters don't have default values
                    param_fields[param_name] = (python_type, Field(description=param_desc))
                else:
                    # Optional parameters have default values if provided, otherwise None
                    param_fields[param_name] = (
                        Optional[python_type], 
                        Field(default=param_default, description=param_desc)
                    )
        
        # If no parameters, return a basic Tool
        if not param_fields:
            def tool_func(input_text: str) -> str:
                try:
                    execute_url = "http://localhost:8080/api/mcp/execute"
                    payload = {
                        "toolName": name,
                        "parameters": {}
                    }
                    
                    response = httpx.post(execute_url, json=payload, timeout=10)
                    
                    if response.status_code == 200:
                        result = response.json()
                        if isinstance(result, dict):
                            return json.dumps(result, indent=2)
                        return str(result)
                    else:
                        return f"Error calling MCP tool: {response.status_code} - {response.text}"
                except Exception as e:
                    return f"Error executing MCP tool: {str(e)}"
            
            return Tool(
                name=operation_id,
                description=f"{display_name}: {description}",
                func=tool_func
            )
        
        # Create a dynamic Pydantic model for the parameters
        ParamModel = create_model(
            f"{operation_id.capitalize()}Params",
            **param_fields
        )
        
        # Function that will be called by the StructuredTool
        def tool_func(**kwargs) -> str:
            try:
                # Convert kwargs to parameters dict
                params = {}
                for key, value in kwargs.items():
                    if value is not None:  # Only include non-None values
                        params[key] = value
                
                # Call the MCP execute endpoint
                execute_url = "http://localhost:8080/api/mcp/execute"
                payload = {
                    "toolName": name,
                    "parameters": params
                }
                
                response = httpx.post(execute_url, json=payload, timeout=10)
                
                # Return formatted response
                if response.status_code == 200:
                    result = response.json()
                    # Prettify the response if it's JSON
                    if isinstance(result, dict):
                        return json.dumps(result, indent=2)
                    return str(result)
                else:
                    return f"Error calling MCP tool: {response.status_code} - {response.text}"
                    
            except Exception as e:
                return f"Error executing MCP tool: {str(e)}"
        
        # Build parameter descriptions for tool documentation
        param_docs = []
        for param in parameters:
            param_name = param.get("name")
            if not param_name:
                continue
                
            required = "required" if param.get("required", False) else "optional"
            default = f", default: {param.get('defaultValue')}" if param.get("defaultValue") is not None else ""
            example = f", example: {param.get('example')}" if param.get("example") is not None else ""
            desc = param.get("description", "")
            
            param_docs.append(f"- {param_name} ({required}{default}{example}): {desc}")
        
        # Construct tool description
        tool_description = f"{display_name}: {description}\n\nParameters:\n" + "\n".join(param_docs)
        
        # Create the StructuredTool with the dynamic model
        return StructuredTool.from_function(
            func=tool_func,
            name=operation_id,
            description=tool_description,
            args_schema=ParamModel
        )
    except Exception as e:
        logging.error(f"Error building tool: {str(e)}")
        return None

def load_mcp_tools(swagger_url: str) -> List[Union[Tool, StructuredTool]]:
    """
    Load MCP tools from the API with detailed parameter information
    
    Args:
        swagger_url: URL to the MCP tools endpoint
        
    Returns:
        List of langchain Tools configured for MCP
    """
    try:
        # Fetch the tools from the MCP API
        response = httpx.get(swagger_url, timeout=10)
        if response.status_code != 200:
            logging.error(f"Failed to fetch MCP tools: {response.status_code} - {response.text}")
            raise Exception(f"Failed to fetch MCP tools: {response.status_code}")
            
        response_data = response.json()
        
        # Extract the tools array from the response
        if response_data.get("status") != "success" or "data" not in response_data:
            logging.error(f"Invalid MCP tools response: {response_data}")
            raise Exception("Invalid MCP tools response format")
            
        tools_data = response_data.get("data", [])
        if not isinstance(tools_data, list):
            logging.error(f"Invalid MCP tools data format: {tools_data}")
            raise Exception("Invalid MCP tools data format")
        
        # Build Tool objects for each MCP tool with detailed information
        tools = []
        for tool_data in tools_data:
            try:
                tool_name = tool_data.get("name")
                if not tool_name:
                    logging.warning(f"Tool missing name: {tool_data}")
                    continue
                    
                # Fetch detailed tool information
                tool_details = get_tool_details(tool_name)
                
                tool = build_mcp_tool(tool_data, tool_details)
                if tool:
                    tools.append(tool)
            except Exception as e:
                logging.error(f"Error processing tool {tool_data.get('name')}: {str(e)}")
                # Continue with other tools if one fails
                continue
                
        logging.info(f"Loaded {len(tools)} MCP tools")
        return tools
        
    except Exception as e:
        logging.error(f"Error loading MCP tools from {swagger_url}: {str(e)}")
        raise
