#!/usr/bin/env python3
import os
import sys

# Add project root to Python path
project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
sys.path.insert(0, project_root)

import json
from typing import Dict, Any, Optional
from dotenv import load_dotenv
from colorama import Fore, Style, init as colorama_init

from langchain_core.tools import tool, ToolException
from langchain_core.messages import AIMessage, HumanMessage
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_openai import ChatOpenAI
from langchain.agents import AgentExecutor, create_openai_tools_agent

from api_tool.api_parser import load_openapi_spec, get_all_endpoints_documentation
from api_tool.api_service import APIService
from api_tool.mcp_client import MCPClient

# Initialize colorama for colored terminal output
colorama_init()

# Load environment variables from .env file
load_dotenv()

# OpenAI API Key
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
if not OPENAI_API_KEY:
    print(f"{Fore.RED}Error: OPENAI_API_KEY environment variable is not set.{Style.RESET_ALL}")
    print("Create a .env file with your OpenAI API key or set it as an environment variable.")
    sys.exit(1)

# Load OpenAPI spec
OPENAPI_PATH = "../resources/openapi.yaml"
try:
    openapi_spec = load_openapi_spec(OPENAPI_PATH)
    api_documentation = get_all_endpoints_documentation(openapi_spec)
except Exception as e:
    print(f"{Fore.RED}Error loading OpenAPI specification: {str(e)}{Style.RESET_ALL}")
    sys.exit(1)

# Get server URL from OpenAPI spec
DEFAULT_SERVER_URL = openapi_spec.get('servers', [{}])[0].get('url', 'http://localhost:8080')
SERVER_URL = os.getenv("API_SERVER_URL", DEFAULT_SERVER_URL)
API_KEY = os.getenv("API_KEY", "")

# Initialize API service and MCP client
api_service = APIService(SERVER_URL, API_KEY)
mcp_client = MCPClient(SERVER_URL)

# Define tools for API endpoints
@tool
def call_api_endpoint(method: str, path: str, params: Optional[Dict[str, Any]] = None, data: Optional[Dict[str, Any]] = None) -> str:
    """
    Call an API endpoint with the specified method, path, params, and data.
    
    Args:
        method: The HTTP method (GET, POST, PUT, DELETE, PATCH)
        path: The API endpoint path
        params: Query parameters as a dictionary
        data: Request body data as a dictionary
    
    Returns:
        API response as a JSON string
    """
    try:
        response = api_service.call_api(method, path, params, data)
        return json.dumps(response, indent=2)
    except Exception as e:
        raise ToolException(f"Error calling API: {str(e)}")

@tool
def get_available_endpoints() -> str:
    """
    Get documentation for all available API endpoints.
    
    Returns:
        Documentation of all available API endpoints
    """
    return api_documentation

@tool
def execute_search_products(category: Optional[str] = None, seller_id: Optional[str] = None) -> str:
    """
    Search for products with optional category and seller ID filters.
    
    Args:
        category: Filter products by category
        seller_id: Filter products by seller ID
    
    Returns:
        List of products matching the criteria
    """
    try:
        params = {}
        if category:
            params['category'] = category
        if seller_id:
            params['sellerId'] = seller_id
            
        response = api_service.call_api("GET", "/v1/products", params=params)
        return json.dumps(response, indent=2)
    except Exception as e:
        raise ToolException(f"Error searching products: {str(e)}")

# Define MCP tools
@tool
def get_mcp_tools() -> str:
    """
    Get a list of all available MCP tools.
    
    Returns:
        Description of all available MCP tools
    """
    try:
        return mcp_client.describe_tools()
    except Exception as e:
        raise ToolException(f"Error getting MCP tools: {str(e)}")

@tool
def get_mcp_tool_details(tool_name: str) -> str:
    """
    Get detailed information about a specific MCP tool.
    
    Args:
        tool_name: Name of the MCP tool
    
    Returns:
        Detailed information about the specified tool
    """
    try:
        details = mcp_client.get_tool_details(tool_name)
        return json.dumps(details, indent=2)
    except Exception as e:
        raise ToolException(f"Error getting tool details: {str(e)}")

@tool
def execute_mcp_tool(tool_name: str, parameters: Optional[Dict[str, Any]] = None) -> str:
    """
    Execute a specific MCP tool with the given parameters.
    
    Args:
        tool_name: Name of the MCP tool to execute
        parameters: Parameters for the tool execution
    
    Returns:
        Tool execution results
    """
    try:
        result = mcp_client.execute_tool(tool_name, parameters)
        return json.dumps(result, indent=2)
    except Exception as e:
        raise ToolException(f"Error executing MCP tool: {str(e)}")

# Define all tools
tools = [
    call_api_endpoint,
    get_available_endpoints,
    execute_search_products,
    get_mcp_tools,
    get_mcp_tool_details,
    execute_mcp_tool
]

# Create the LLM
llm = ChatOpenAI(
    api_key=OPENAI_API_KEY,
    model="gpt-4",
    temperature=0
)

# Create the system message with API and MCP documentation
system_message = f"""
You are an AI assistant designed to help users interact with both the E-commerce Product Prediction API and MCP tools.
You can perform API calls and execute MCP tools on behalf of the user and provide insights based on the results.

Here's the documentation of available API endpoints:

{api_documentation}

For MCP tools, you can:
1. Use get_mcp_tools() to list all available MCP tools
2. Use get_mcp_tool_details(tool_name) to get detailed information about a specific tool
3. Use execute_mcp_tool(tool_name, parameters) to execute a tool

Remember to:
- Call the appropriate API endpoint or MCP tool based on user requests
- If you're unsure about the API capabilities, use the get_available_endpoints tool
- If you're unsure about MCP tools, use the get_mcp_tools tool
- Always verify tool parameters before execution

Always respond in a helpful, clear manner. If you can't fulfill a request using the available endpoints or tools,
explain why and suggest alternatives if possible.
"""

# Create the prompt template with agent_scratchpad
prompt = ChatPromptTemplate.from_messages([
    ("system", system_message),
    ("human", "{input}"),
    MessagesPlaceholder(variable_name="agent_scratchpad"),
])

# Create an agent with the tools
agent = create_openai_tools_agent(llm, tools, prompt)
agent_executor = AgentExecutor(agent=agent, tools=tools, verbose=False)

def main():
    print(f"{Fore.GREEN}=== E-commerce API and MCP Chatbot ==={Style.RESET_ALL}")
    print(f"{Fore.YELLOW}Type 'exit' to quit{Style.RESET_ALL}")
    print(f"Connected to server: {SERVER_URL}")
    
    # Check MCP server status
    try:
        status = mcp_client.get_server_status()
        print(f"MCP server status: {status['status']}, version: {status['version']}")
        print(f"Available MCP tools: {status['toolCount']}")
    except Exception as e:
        print(f"{Fore.RED}Warning: Could not connect to MCP server: {str(e)}{Style.RESET_ALL}")
    
    chat_history = []
    
    while True:
        try:
            # Get user input
            user_input = input(f"{Fore.BLUE}You: {Style.RESET_ALL}")
            
            # Exit condition
            if user_input.lower() in ('exit', 'quit'):
                print(f"{Fore.GREEN}Goodbye!{Style.RESET_ALL}")
                break
            
            # Add to history
            chat_history.append(HumanMessage(content=user_input))
            
            # Run the agent executor
            response = agent_executor.invoke({"input": user_input})
            output = response.get("output", "I couldn't process your request.")
            
            # Add to history
            chat_history.append(AIMessage(content=output))
            
            # Print response
            print(f"{Fore.GREEN}Assistant: {Style.RESET_ALL}{output}")
            
        except KeyboardInterrupt:
            print(f"{Fore.GREEN}\nGoodbye!{Style.RESET_ALL}")
            break
        except Exception as e:
            print(f"{Fore.RED}Error: {str(e)}{Style.RESET_ALL}")

if __name__ == "__main__":
    main() 