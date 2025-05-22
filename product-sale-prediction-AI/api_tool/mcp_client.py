import requests
import json
from typing import Dict, List, Any, Optional, Union


class MCPClient:
    """用于与MCP服务器交互的Python客户端"""
    
    def __init__(self, base_url: str = "http://localhost:8080"):
        """
        初始化MCP客户端
        
        Args:
            base_url: MCP服务器的基础URL
        """
        self.base_url = base_url.rstrip('/')
        self.api_path = f"{self.base_url}/api/mcp"
        self.tools_cache = None  # 缓存工具列表
        
    def get_server_status(self) -> Dict:
        """获取MCP服务器状态"""
        response = requests.get(f"{self.api_path}/status")
        response.raise_for_status()
        return response.json()
        
    def list_tools(self, refresh: bool = False) -> List[Dict]:
        """
        获取所有可用工具列表
        
        Args:
            refresh: 是否刷新缓存
        
        Returns:
            工具列表
        """
        if self.tools_cache is None or refresh:
            response = requests.get(f"{self.api_path}/tools")
            response.raise_for_status()
            result = response.json()
            self.tools_cache = result.get('data', [])
        return self.tools_cache
    
    def get_tool_details(self, tool_name: str) -> Dict:
        """
        获取特定工具的详细信息
        
        Args:
            tool_name: 工具名称
            
        Returns:
            工具详细信息
        """
        response = requests.get(f"{self.api_path}/tools/{tool_name}")
        response.raise_for_status()
        result = response.json()
        return result.get('data', {})
    
    def execute_tool(self, tool_name: str, parameters: Dict = None) -> Dict:
        """
        执行一个工具
        
        Args:
            tool_name: 工具名称
            parameters: 工具参数
            
        Returns:
            执行结果
        """
        if parameters is None:
            parameters = {}
            
        request_body = {
            "toolName": tool_name,
            "parameters": parameters
        }
        
        response = requests.post(f"{self.api_path}/execute", json=request_body)
        response.raise_for_status()
        return response.json()
    
    def describe_tools(self) -> str:
        """返回所有工具的描述信息，以便于查看"""
        tools = self.list_tools()
        output = "可用的MCP工具:\n"
        
        for tool in tools:
            output += f"- {tool['name']} ({tool['displayName']}): {tool['description']}\n"
            
        output += "\n使用 get_tool_details(tool_name) 获取特定工具的参数详情"
        return output


# 示例使用方法
if __name__ == "__main__":
    # 创建客户端
    mcp = MCPClient("http://localhost:8080")
    
    # 获取服务器状态
    status = mcp.get_server_status()
    print(f"MCP服务器状态: {status['status']}, 版本: {status['version']}")
    print(f"可用工具数量: {status['toolCount']}")
    
    # 显示所有可用工具
    print("\n" + mcp.describe_tools())
    
    # 获取特定工具的详细信息
    product_tool = mcp.get_tool_details("getProducts")
    print(f"\n'{product_tool['displayName']}'工具详情:")
    print(f"  描述: {product_tool['description']}")
    print("  参数:")
    for param in product_tool.get('parameters', []):
        required = "必填" if param.get('required', False) else "可选"
        print(f"    - {param['name']} ({param['type']}): {param['description']} [{required}]")
    
    # 使用工具示例 - 获取产品列表
    try:
        result = mcp.execute_tool("getProducts", {"category": "Electronics"})
        if result['status'] == 'success':
            data = result['data']
            print(f"\n查询到 {data['count']} 个电子产品")
        else:
            print(f"\n执行失败: {result.get('error', '未知错误')}")
    except Exception as e:
        print(f"发生错误: {str(e)}")