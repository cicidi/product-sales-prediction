import logging
import json
import os
from typing import List, Dict, Any, Optional
from datetime import datetime

from langchain.memory import ConversationBufferMemory
from langchain.schema import BaseChatMessageHistory, AIMessage, HumanMessage
from langchain_openai import ChatOpenAI

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
      logging.FileHandler("agent_memory.log"),
      logging.StreamHandler()
    ]
)
logger = logging.getLogger("memory_module")


class EnhancedMemory:
  """
  Enhanced memory module that maintains conversation history across sessions,
  detects missing context, and provides context enhancement.
  """
  def __init__(self,llm: ChatOpenAI, session_id: str = "default",
      history_limit: int = 10) -> None:
    """
    Initialize the enhanced memory module.

    Args:
        session_id: Unique identifier for the conversation session
        history_limit: Number of recent conversation entries to include in context
    """
    self.session_id = session_id
    self.history_limit = history_limit
    self.chat_history = []
    self.memory = ConversationBufferMemory(memory_key="chat_history",
                                           return_messages=True)
    self.thought_log = []
    self.llm = llm

    # Load existing history if available
    self._load_history()

  def _load_history(self):
    """Load conversation history from storage."""
    try:
      if os.path.exists(f"session_{self.session_id}_history.json"):
        with open(f"session_{self.session_id}_history.json", "r") as f:
          self.chat_history = json.load(f)

        # Populate the LangChain memory with recent history
        self._update_langchain_memory()
        logger.info(
          f"Loaded history for session {self.session_id} with {len(self.chat_history)} entries")
    except Exception as e:
      logger.error(f"Error loading history: {str(e)}")

  def _save_history(self):
    """Save conversation history to storage."""
    try:
      with open(f"session_{self.session_id}_history.json", "w") as f:
        json.dump(self.chat_history, f, indent=2)
      logger.info(f"Saved history for session {self.session_id}")
    except Exception as e:
      logger.error(f"Error saving history: {str(e)}")

  def _update_langchain_memory(self):
    """Update the LangChain memory with recent conversation history."""
    # Clear existing memory
    self.memory.clear()

    # Get recent history (limited by history_limit)
    recent_history = self.chat_history[-self.history_limit:] if len(
      self.chat_history) > self.history_limit else self.chat_history

    # Add messages to memory
    for entry in recent_history:
      self.memory.chat_memory.add_user_message(entry["user"])
      self.memory.chat_memory.add_ai_message(entry["reply"])

    logger.info(
      f"Updated LangChain memory with {len(recent_history)} recent conversations")

  def add_interaction(self, user_input: str, agent_reply: str):
    """
    Add a new user-agent interaction to the history.

    Args:
        user_input: The user's message
        agent_reply: The agent's response
    """
    # Add to internal history
    self.chat_history.append({
      "user": user_input,
      "reply": agent_reply,
      "timestamp": datetime.now().isoformat()
    })

    # Add to LangChain memory
    self.memory.chat_memory.add_user_message(user_input)
    self.memory.chat_memory.add_ai_message(agent_reply)

    # Save updated history
    self._save_history()

  def log_thought(self, thought: str):
    """
    Log agent's thought process.

    Args:
        thought: The agent's reasoning or thought process
    """
    timestamp = datetime.now().isoformat()
    self.thought_log.append({
      "timestamp": timestamp,
      "thought": thought
    })
    logger.info(f"Agent thought: {thought}")

  def get_memory_for_agent(self):
    """Get the LangChain memory object for use with the agent."""
    return self.memory

  def is_missing_context(self, agent_reply: str) -> bool:
    """
    Detect if the agent is missing context based on its reply using an LLM.

    Args:
        agent_reply: The agent's response to analyze

    Returns:
        True if context appears to be missing, False otherwise
    """
    # Define the prompt for the LLM
    prompt = f"""
        Analyze the following response and determine if it indicates that the agent is missing context or asking for more information.:
    
        Response: "{agent_reply}"
    
        If the response suggests missing context or asking more information, reply with "True". Otherwise, reply with "False".
        """

    try:
      # Use the LLM to analyze the response
      llm_response = self.llm.predict(prompt).strip()
      logger.info(f": agent reply: {agent_reply} : and agent need more information from user: {llm_response}")
      return llm_response.lower() == "true"
    except Exception as e:
      logger.error(f"Error while using LLM to detect missing context: {str(e)}")
    return False

  def enhance_context(self, user_input: str,agent_reply:str) -> str:
    """
    Enhance the user input with full conversation history when context is missing.

    Args:
        user_input: The original user input

    Returns:
        Enhanced user input with conversation history prepended
    """
    # Create a summary of the full conversation history
    history_summary = "\n\n===== CONVERSATION HISTORY =====\n"
    for idx, entry in enumerate(self.chat_history):
      history_summary += f"User: {entry['user']}\n"
      history_summary += f"Assistant: {entry['reply']}\n\n"

    # Combine history with the current query
    enhanced_input = (
      f"{history_summary}\n"
      f"===== CURRENT QUERY =====\n"
      f"User: {user_input}\n\n"
      f"As an AI assistant, you are provided with the full conversation history above. Use this context to determine whether you still need to ask the user any follow-up questions in order to complete the current task. Try to infer your next action based on the entire conversation.\n\n"
      f"Note: You may have previously asked the user to provide some information. Now that the user has responded, make sure you remember what your original goal or task was, and continue accordingly.\n"
      f"### Proposed Answer: {agent_reply} ###\n\n"
      f"Instructions:\n"
      f"- If the conversation history already provides sufficient information to complete the task, proceed accordingly.\n"
      f"- If the necessary information is missing or unclear, explicitly respond with 'I don't know' or ask the user for the specific information required.\n"
      f"- Do NOT fabricate or assume any information that is not clearly present in the conversation history.\n"
    )



    logger.info("Enhanced user input with full conversation history")
    return enhanced_input

  def get_thought_log(self) -> List[Dict[str, str]]:
    """Get the agent's thought log."""
    return self.thought_log
