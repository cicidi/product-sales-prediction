
# chat_sales_assistant.py

# Install python-dotenv if not already installed
# pip install python-dotenv

import os
from dotenv import load_dotenv
import streamlit as st
from langchain.agents import initialize_agent
from langchain.agents.agent_types import AgentType
from langchain.chat_models import ChatOpenAI
from tools.loader import load_mcp_tools

st.set_page_config(page_title="Sales Assistant", layout="wide")
st.title("🛍️ AI 销售助理")

if "chat_history" not in st.session_state:
    st.session_state.chat_history = []

llm = ChatOpenAI(temperature=0)

with st.spinner("加载工具中..."):
    tools: list[Tool] = load_mcp_tools(base_url="http://localhost:8080")

agent = initialize_agent(
    tools=tools,
    llm=llm,
    agent=AgentType.OPENAI_FUNCTIONS,
    verbose=True
)

user_input = st.chat_input("请输入你的问题，例如：最近什么产品卖得最好？")
if user_input:
    st.session_state.chat_history.append(("user", user_input))
    with st.spinner("🤖 正在思考中..."):
        response = agent.run(user_input)
        st.session_state.chat_history.append(("ai", response))

for role, msg in st.session_state.chat_history:
    if role == "user":
        st.chat_message("user").write(msg)
    else:
        st.chat_message("ai").write(msg)
