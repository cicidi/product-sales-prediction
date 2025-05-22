"""
AI Sales Assistant

A Streamlit-based chatbot that uses LangChain and OpenAI to help sellers analyze sales data,
make predictions, and visualize information through natural language interaction.
"""

import os
from typing import Dict, Any, List, Tuple, Optional
import streamlit as st
from dotenv import load_dotenv
import pandas as pd
import plotly.express as px
import plotly.graph_objects as go
from langchain.agents import initialize_agent
from langchain.agents.agent_types import AgentType
from langchain_openai import ChatOpenAI
from langchain.schema import SystemMessage, HumanMessage, AIMessage
from tools.mcp_tool_loader import load_mcp_tools
import logging
import json

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Load environment variables
load_dotenv()

class ChatHistory:
    """Manages chat history and context."""
    
    def __init__(self):
        if "messages" not in st.session_state:
            st.session_state.messages = []
        if "context" not in st.session_state:
            st.session_state.context = {}
        if "last_api_response" not in st.session_state:
            st.session_state.last_api_response = None
    
    def add_message(self, role: str, content: str, data: Any = None):
        """Add a message to the chat history."""
        message = {"role": role, "content": content}
        if data is not None:
            message["data"] = data
        st.session_state.messages.append(message)
    
    def get_context(self) -> Dict[str, Any]:
        """Get the current conversation context."""
        return st.session_state.context
    
    def update_context(self, key: str, value: Any):
        """Update the conversation context."""
        st.session_state.context[key] = value
    
    def clear(self):
        """Clear the chat history and context."""
        st.session_state.messages = []
        st.session_state.context = {}
        st.session_state.last_api_response = None

class DataVisualizer:
    """Handles data visualization using Plotly."""
    
    @staticmethod
    def create_chart(data: Any, chart_type: str = None) -> go.Figure:
        """Create an appropriate chart based on the data structure."""
        if isinstance(data, list):
            df = pd.DataFrame(data)
        elif isinstance(data, dict):
            df = pd.DataFrame([data])
        else:
            return None
        
        # Detect numeric columns
        numeric_cols = df.select_dtypes(include=['int64', 'float64']).columns
        if len(numeric_cols) == 0:
            return None
        
        # Choose chart type based on data structure
        if chart_type == "bar" or (not chart_type and len(df) <= 10):
            fig = px.bar(df, x=df.index, y=numeric_cols[0])
        elif chart_type == "line" or (not chart_type and len(df) > 10):
            fig = px.line(df, y=numeric_cols)
        else:
            fig = px.bar(df, x=df.index, y=numeric_cols[0])
        
        fig.update_layout(
            template="plotly_white",
            margin=dict(l=20, r=20, t=40, b=20),
            title_x=0.5
        )
        return fig
    
    @staticmethod
    def display_data(data: Any):
        """Display data with appropriate visualizations."""
        if not data:
            return
        
        # Try to create a chart
        fig = DataVisualizer.create_chart(data)
        if fig:
            st.plotly_chart(fig, use_container_width=True)
        
        # Display raw data
        with st.expander("View Raw Data"):
            st.write(data)
            
            # If data is tabular, show summary statistics
            if isinstance(data, list) and len(data) > 0 and isinstance(data[0], dict):
                df = pd.DataFrame(data)
                numeric_cols = df.select_dtypes(include=['int64', 'float64']).columns
                if len(numeric_cols) > 0:
                    st.write("Summary Statistics:")
                    st.write(df[numeric_cols].describe())

class ToolManager:
    """Manages MCP tools and their execution."""
    
    def __init__(self, base_url: str):
        self.base_url = base_url
        self.tools = []
        self.load_tools()
    
    def load_tools(self):
        """Load all available MCP tools."""
        try:
            self.tools = load_mcp_tools(base_url=self.base_url)
            logger.info(f"Loaded {len(self.tools)} MCP tools")
        except Exception as e:
            logger.error(f"Failed to load MCP tools: {str(e)}")
            self.tools = []
    
    def find_suitable_tool(self, query: str) -> Optional[Tool]:
        """Find the most suitable tool for the given query."""
        # First, try to match based on tool description
        for tool in self.tools:
            if any(keyword in query.lower() for keyword in tool.description.lower().split()):
                return tool
        return None
    
    def execute_tool(self, tool: Tool, **kwargs) -> Dict[str, Any]:
        """Execute a tool with the given parameters."""
        try:
            result = tool.func(**kwargs)
            return {
                "success": True,
                "data": result,
                "message": f"Successfully executed {tool.name}"
            }
        except Exception as e:
            logger.error(f"Error executing tool {tool.name}: {str(e)}")
            return {
                "success": False,
                "error": str(e),
                "message": f"Failed to execute {tool.name}"
            }

class SalesAssistant:
    """Main sales assistant class that handles the chat interface and tool interactions."""
    
    def __init__(self):
        self.chat_history = ChatHistory()
        self.visualizer = DataVisualizer()
        self.tool_manager = ToolManager(base_url="http://localhost:8080")
        
        # Initialize OpenAI for natural language understanding
        self.llm = ChatOpenAI(
            temperature=0,
            model="gpt-4-turbo-preview"
        )
        
        # Setup Streamlit UI
        self.setup_ui()
    
    def setup_ui(self):
        """Setup the Streamlit user interface."""
        st.set_page_config(
            page_title="AI Sales Assistant",
            page_icon="🛍️",
            layout="wide"
        )
        
        # Custom CSS
        st.markdown("""
            <style>
            .stApp {
                max-width: 1200px;
                margin: 0 auto;
            }
            .chat-message {
                padding: 1rem;
                border-radius: 0.5rem;
                margin-bottom: 1rem;
                display: flex;
                flex-direction: column;
            }
            .user-message {
                background-color: #e3f2fd;
                margin-left: 2rem;
            }
            .assistant-message {
                background-color: #f5f5f5;
                margin-right: 2rem;
            }
            </style>
        """, unsafe_allow_html=True)
        
        # Main title
        st.title("🛍️ AI Sales Assistant")
        st.markdown("---")
        
        # Sidebar
        with st.sidebar:
            st.header("Settings")
            if st.button("Clear Chat History"):
                self.chat_history.clear()
                st.rerun()
            
            with st.expander("Available Tools"):
                for tool in self.tool_manager.tools:
                    st.write(f"**{tool.name}**")
                    st.write(tool.description)
                    st.markdown("---")
    
    def process_message(self, message: str) -> Tuple[str, Any]:
        """Process a user message and return the response and any API data."""
        try:
            # Get conversation context
            context = self.chat_history.get_context()
            
            # Find suitable tool
            tool = self.tool_manager.find_suitable_tool(message)
            if not tool:
                return "I'm sorry, I couldn't find a suitable tool to help with your request.", None
            
            # Use OpenAI to extract parameters from the message
            prompt = f"""
            Extract parameters for the tool: {tool.name}
            Tool description: {tool.description}
            User message: {message}
            Previous context: {json.dumps(context) if context else 'None'}
            
            Return a JSON object with the parameter values. Only include parameters mentioned in the message or context.
            """
            
            completion = self.llm.invoke([HumanMessage(content=prompt)])
            try:
                params = json.loads(completion.content)
            except:
                params = {}
            
            # Execute the tool
            result = self.tool_manager.execute_tool(tool, **params)
            
            if result["success"]:
                response = f"I found some information for you using the {tool.name} tool.\n\n"
                if isinstance(result["data"], dict) and "error" in result["data"]:
                    response += f"However, there was an error: {result['data']['error']}"
                    return response, None
                else:
                    return response, result["data"]
            else:
                return f"I encountered an error while trying to help you: {result['error']}", None
            
        except Exception as e:
            logger.error(f"Error processing message: {str(e)}")
            return f"I encountered an error while processing your request: {str(e)}", None
    
    def run(self):
        """Run the chat interface."""
        # Display chat history
        for message in st.session_state.messages:
            with st.chat_message(message["role"]):
                st.write(message["content"])
                if message["role"] == "assistant" and "data" in message:
                    self.visualizer.display_data(message["data"])
        
        # Chat input
        if user_input := st.chat_input("Ask me anything about sales data..."):
            # Add user message to chat
            self.chat_history.add_message("user", user_input)
            st.chat_message("user").write(user_input)
            
            # Get AI response
            with st.chat_message("assistant"):
                with st.spinner("Thinking..."):
                    response, api_data = self.process_message(user_input)
                    
                    # Display response
                    st.write(response)
                    if api_data:
                        self.visualizer.display_data(api_data)
                    
                    # Add to chat history
                    self.chat_history.add_message("assistant", response, api_data)

if __name__ == "__main__":
    # Ensure OpenAI API key is set
    if not os.getenv("OPENAI_API_KEY"):
        st.error("Please set your OPENAI_API_KEY in the .env file")
        st.stop()
    
    # Run the assistant
    assistant = SalesAssistant()
    assistant.run() 