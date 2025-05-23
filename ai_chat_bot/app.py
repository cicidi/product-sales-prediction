import os
import uuid

import streamlit as st
from dotenv import load_dotenv

from agent_initializer import build_agent

load_dotenv()
openai_api_key = os.getenv("OPENAI_API_KEY")

st.set_page_config(page_title="AI sale assistant", layout="wide")
st.title("AI Chatbot")

# Initialize session state for user ID
if "user_id" not in st.session_state:
    st.session_state.user_id = str(uuid.uuid4())
    st.sidebar.info(f"Session ID: {st.session_state.user_id}")

# Initialize chat history in session state
if "chat_history" not in st.session_state:
    st.session_state.chat_history = []

# Initialize agent with session ID
if "agent" not in st.session_state:
    st.session_state.agent = build_agent(
        "http://localhost:8080/api/mcp/tools",
        session_id=st.session_state.user_id
    )

# Display the chat history in the UI
for item in st.session_state.chat_history:
    with st.chat_message("user"):
        st.markdown(item["user"])
    with st.chat_message("assistant"):
        st.markdown(item["reply"])

# Display thought log in sidebar if debug mode is enabled
if st.sidebar.checkbox("Show Agent Thought Process", value=False):
    if isinstance(st.session_state.agent, object) and hasattr(st.session_state.agent, 'memory_module'):
        thought_log = st.session_state.agent.memory_module.get_thought_log()
        if thought_log:
            st.sidebar.subheader("Agent Thought Process")
            for thought in thought_log:
                st.sidebar.text(f"{thought['timestamp']}: {thought['thought']}")
        else:
            st.sidebar.info("No thought logs available yet.")

# Get user input
user_input = st.chat_input("Enter your question, like: what is my last month best sale product？")

if user_input:
    # Display user message
    st.chat_message("user").markdown(user_input)

    try:
        # Process through our enhanced agent
        reply = st.session_state.agent.run(user_input)
    except Exception as e:
        reply = f"出错了：{str(e)}"

    # Display assistant response
    st.chat_message("assistant").markdown(reply)

    # Add to Streamlit session state for UI display
    st.session_state.chat_history.append({
        "user": user_input,
        "reply": reply
    })
