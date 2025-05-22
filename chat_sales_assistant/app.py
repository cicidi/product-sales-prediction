import os

import streamlit as st
from dotenv import load_dotenv

from agent_initializer import build_agent

load_dotenv()
openai_api_key = os.getenv("OPENAI_API_KEY")

st.set_page_config(page_title="AI sale assistant", layout="wide")
st.title("AI Chatbot")

if "chat_history" not in st.session_state:
    st.session_state.chat_history = []
if "agent" not in st.session_state:
    st.session_state.agent = build_agent("http://localhost:8080/api/mcp/tools")

for item in st.session_state.chat_history:
    with st.chat_message("user"):
        st.markdown(item["user"])
    with st.chat_message("assistant"):
        st.markdown(item["reply"])

user_input = st.chat_input("Enter your question, like: what is my last month best sale product？")

if user_input:
    st.chat_message("user").markdown(user_input)
    try:
        reply = st.session_state.agent.run(user_input)
    except Exception as e:
        reply = f"出错了：{str(e)}"

    st.chat_message("assistant").markdown(reply)

    st.session_state.chat_history.append({
        "user": user_input,
        "reply": reply
    })
