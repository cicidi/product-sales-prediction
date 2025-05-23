from langchain_openai import ChatOpenAI
from time_util import resolve_time_tool
from langchain.memory import ConversationBufferMemory
from langchain.agents import initialize_agent, AgentType
from mcp_tool_loader import load_mcp_tools
from memory_module import EnhancedMemory
import logging

# Configure logging for agent thoughts
logger = logging.getLogger("agent_initializer")

class EnhancedAgent:
    """
    Enhanced agent that incorporates the memory module for context management
    and provides capabilities for context enhancement and reasoning logging.
    """

    def __init__(self, swagger_url: str, session_id: str = "default"):
        """
        Initialize the enhanced agent.
        
        Args:
            swagger_url: URL for loading MCP tools
            session_id: Unique identifier for the conversation session
        """
        self.swagger_url = swagger_url
        self.session_id = session_id
        self.tools = [resolve_time_tool] + load_mcp_tools(swagger_url)
        self.llm = ChatOpenAI(temperature=0, model="gpt-4o")
        self.memory_module = EnhancedMemory(self.llm,session_id=session_id)
        # Initialize the agent with our memory
        self.agent = initialize_agent(
            tools=self.tools,
            llm=self.llm,
            agent=AgentType.OPENAI_FUNCTIONS,
            memory=self.memory_module.get_memory_for_agent(),
            verbose=True,
            handle_parsing_errors=True,
        )

    def run(self, user_input: str) -> str:
        """
        Run the agent with enhanced context management.
        
        Args:
            user_input: The user's query
            
        Returns:
            The agent's response
        """
        # First attempt with standard input
        logger.info(f"Processing user input: {user_input}")
        self.memory_module.log_thought(f"Initial query: {user_input}")

        try:
            # First pass - use normal input
            agent_reply = self.agent.run(user_input)
            logger.info(f"Initial agent response: {agent_reply}")

            # Check if context might be missing
            if self.memory_module.is_missing_context(agent_reply):
                self.memory_module.log_thought("Context missing detected. Enhancing input with conversation history.")

                # Second pass - use enhanced input
                enhanced_input = self.memory_module.enhance_context(user_input,agent_reply)
                self.memory_module.log_thought(f"Enhanced input created: {enhanced_input[:100]}...")

                # Run with enhanced context
                agent_reply = self.agent.run(enhanced_input)
                self.memory_module.log_thought(f"Response with enhanced context: {agent_reply}")

            # Record the interaction in memory
            self.memory_module.add_interaction(user_input, agent_reply)
            return agent_reply

        except Exception as e:
            error_msg = f"Error running agent: {str(e)}"
            logger.error(error_msg)
            self.memory_module.log_thought(error_msg)
            return f"出错了：{str(e)}"

def build_agent(swagger_url: str, session_id: str = "default"):
    """
    Build an enhanced agent with memory capabilities.
    
    Args:
        swagger_url: URL for loading MCP tools
        session_id: Unique identifier for the conversation session
        
    Returns:
        An enhanced agent instance
    """
    return EnhancedAgent(swagger_url, session_id)