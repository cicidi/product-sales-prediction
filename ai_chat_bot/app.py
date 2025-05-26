import os
import uuid
from datetime import datetime
import streamlit as st
from dotenv import load_dotenv
from agent_initializer import build_agent
import base64

# Load env variables
load_dotenv()
openai_api_key = os.getenv("OPENAI_API_KEY")

# Page config
st.set_page_config(page_title="AI Sale Assistant", layout="wide")

# Load custom chat CSS
with open("chat_styles.css") as f:
    st.markdown(f"<style>{f.read()}</style>", unsafe_allow_html=True)

# Initialize session state
if "user_id" not in st.session_state:
    st.session_state.user_id = str(uuid.uuid4())
    st.sidebar.info(f"Session ID: {st.session_state.user_id}")

if "chat_history" not in st.session_state:
    st.session_state.chat_history = []

if "agent" not in st.session_state:
    st.session_state.agent = build_agent(
        "http://localhost:8080/api/mcp/tools",
        session_id=st.session_state.user_id
    )

# Sidebar: Common Questions
st.sidebar.subheader("Common Questions")
common_questions = [
    "List all orders for seller_3 in the last month.",
    "Predict seller_1's future sales for product p100 in next 10 days.",
    "What are the details of product ID p101?",
    "Predict the seller 2 's top 3 selling electronics next week.",
    "List all products for seller_5.",
    "Update the price of product ID p300 to 135.0.",
    "What is the total revenue for seller_3 in the last quarter?",
    "How many units of product ID p200 were sold by seller_1 last month?",
    "What are the top 5 best-selling products this year?",
    "What is the average order value for seller_4?"
]
for question in common_questions:
    if st.sidebar.button(question):
        st.session_state.new_user_input = question
        st.rerun()

# Sidebar: Agent Thought Process
if st.sidebar.checkbox("Show Agent Thought Process", value=False):
    if isinstance(st.session_state.agent, object) and hasattr(st.session_state.agent, 'memory_module'):
        thought_log = st.session_state.agent.memory_module.get_thought_log()
        if thought_log:
            st.sidebar.subheader("Agent Thought Process")
            for thought in thought_log:
                st.sidebar.text(f"{thought['timestamp']}: {thought['thought']}")
        else:
            st.sidebar.info("No thought logs available yet.")

# Logo and title
def image_to_base64(filename):
    current_dir = os.path.dirname(__file__)
    path = os.path.join(current_dir, filename)
    with open(path, "rb") as img_file:
        encoded = base64.b64encode(img_file.read()).decode()
        return f"data:image/png;base64,{encoded}"

logo_base64 = image_to_base64("quickbooks.png")
st.markdown(f"""
<div style="display: flex; align-items: center; gap: 0.6rem; margin-bottom: -1rem;">
    <img src="{logo_base64}" alt="QuickBooks Logo" width="40" height="40" style="border-radius: 6px;" />
    <h1 style="margin: 0; font-size: 2rem;">QuickBooks Chatbot Build by AI Agent</h1>
</div>
""", unsafe_allow_html=True)

# Chat container
chat_container = st.container()
with chat_container:
    st.markdown('<div class="chat-container">', unsafe_allow_html=True)
    for item in st.session_state.chat_history:
        timestamp = item.get("timestamp") or datetime.now().strftime("%Y-%m-%d %H:%M")
        st.markdown(f"""
        <div class='chat-row' style='justify-content: flex-end;'>
            <div class='message-block'>
                <div class='message user-message'>{item["user"]}</div>
                <div class='timestamp' style='text-align: right;'>{timestamp}</div>
            </div>
        </div>""", unsafe_allow_html=True)

        st.markdown(f"""
        <div class='chat-row' style='justify-content: flex-start;'>
            <div class='message-block'>
                <div class='message bot-message'>{item["reply"]}</div>
                <div class='timestamp'>{timestamp}</div>
            </div>
        </div>""", unsafe_allow_html=True)
    st.markdown('</div>', unsafe_allow_html=True)

# Handle input (chat_input or preset sample)
user_input = st.chat_input("Enter your question, like: what is my last month best sale product？")
if "new_user_input" in st.session_state:
    user_input = st.session_state.pop("new_user_input")

# Main logic
if user_input:
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M")

    # Display user message
    st.markdown(f"""
    <div class='chat-row' style='justify-content: flex-end;'>
        <div class='message-block'>
            <div class='message user-message'>{user_input}</div>
            <div class='timestamp' style='text-align: right;'>{timestamp}</div>
        </div>
    </div>""", unsafe_allow_html=True)

    try:
        reply = st.session_state.agent.run(user_input)
    except Exception as e:
        reply = f"error: {str(e)}"

    # Display bot message
    st.markdown(f"""
    <div class='chat-row' style='justify-content: flex-start;'>
        <div class='message-block'>
            <div class='message bot-message'>{reply}</div>
            <div class='timestamp'>{timestamp}</div>
        </div>
    </div>""", unsafe_allow_html=True)

    # Save to history
    st.session_state.chat_history.append({
        "user": user_input,
        "reply": reply,
        "timestamp": timestamp
    })
