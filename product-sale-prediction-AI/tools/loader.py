
# tools/loader.py

import requests
from typing import List
from langchain.tools import Tool

def load_mcp_tools(base_url: str) -> List[Tool]:
    tools = []
    tool_list_resp = requests.get(f"{base_url}/api/mcp/tools")
    tool_list_resp.raise_for_status()
    tool_names = tool_list_resp.json()

    for tool_info in tool_names:
        tool_name = tool_info.get("name")
        if not tool_name:
            continue
        detail_resp = requests.get(f"{base_url}/api/mcp/tools/{tool_name}")
        detail_resp.raise_for_status()
        tool_detail = detail_resp.json()
        tool_desc = tool_detail.get("description", f"调用工具：{tool_name}")

        def make_tool_fn(name, desc, required_params):
            def tool_fn(**kwargs):
                payload = {
                    "tool": name,
                    "parameters": kwargs
                }
                resp = requests.post(f"{base_url}/api/mcp/execute", json=payload)
                resp.raise_for_status()
                return resp.json()
            return Tool.from_function(
                func=tool_fn,
                name=name,
                description=desc
            )

        param_defs = tool_detail.get("parameters", [])
        tool_func = make_tool_fn(tool_name, tool_desc, param_defs)
        tools.append(tool_func)

    return tools
