import os

from langchain_openai import ChatOpenAI
from langchain.agents import initialize_agent, AgentType
from mcp_tool_loader import load_mcp_tools
from time_util import  resolve_time_tool


def build_agent(swagger_url: str):
    tools = load_mcp_tools(swagger_url)
    llm = ChatOpenAI(temperature=0, model="gpt-4")
    agent = initialize_agent(
        tools = [resolve_time_tool] + tools,
        llm=llm,
        agent=AgentType.OPENAI_FUNCTIONS,
        verbose=True,
        handle_parsing_errors=True,
    )
    # print(agent.run("请解析：最近三个月"))
    return agent