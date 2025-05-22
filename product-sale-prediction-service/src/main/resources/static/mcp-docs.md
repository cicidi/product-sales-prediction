# Model-Centric Protocol (MCP) API 文档

## 概述

Model-Centric Protocol (MCP) 是一个为大语言模型(LLM)提供的工具调用接口，使AI模型能够通过标准化的协议调用各种功能，如商品销售分析和销售预测。

本API设计遵循工具化思想，将系统功能模块化为可独立调用的"工具"，使大模型能够根据需求进行灵活组合调用。

## API端点

MCP API 提供以下主要端点：

### 1. 获取所有可用工具

**请求**:
```
GET /api/mcp/tools
```

**响应**:
```json
{
  "status": "success",
  "data": [
    {
      "name": "topSellingProducts",
      "displayName": "销售排行分析",
      "description": "分析指定卖家在特定时间段内的销售排行榜，支持按类别筛选，可查询销量最高的商品"
    },
    {
      "name": "predictSales",
      "displayName": "销量预测",
      "description": "基于历史数据预测特定商品在未来时间段的销量和销售额，帮助卖家做出更好的库存和营销决策"
    },
    {
      "name": "searchProducts",
      "displayName": "商品搜索",
      "description": "根据关键字和/或类别搜索商品"
    },
    {
      "name": "searchSimilarProducts",
      "displayName": "相似商品搜索",
      "description": "基于文本描述或商品ID查找相似的商品，使用向量相似度检索技术"
    },
    {
      "name": "getRecentOrders",
      "displayName": "订单列表查询",
      "description": "查询特定卖家的最近订单记录，支持分页功能"
    },
    {
      "name": "getProducts",
      "displayName": "产品列表查询",
      "description": "获取产品列表，支持按类别和卖家ID筛选"
    },
    {
      "name": "getProductById",
      "displayName": "产品详情查询",
      "description": "根据产品ID获取产品详细信息"
    },
    {
      "name": "manageProduct",
      "displayName": "产品管理",
      "description": "创建新产品或更新现有产品信息"
    }
  ],
  "toolName": "list_tools"
}
```

### 2. 获取工具详细信息

**请求**:
```
GET /api/mcp/tools/{toolName}
```

例如:
```
GET /api/mcp/tools/predictSales
```

**响应**:
```json
{
  "status": "success",
  "data": {
    "name": "predictSales",
    "displayName": "销量预测",
    "description": "基于历史数据预测特定商品在未来时间段的销量和销售额，帮助卖家做出更好的库存和营销决策",
    "parameters": [
      {
        "name": "productId",
        "type": "string",
        "description": "商品ID，必填参数，指定需要预测销量的商品",
        "required": true,
        "example": "P123456"
      },
      {
        "name": "sellerId",
        "type": "string",
        "description": "卖家ID，必填参数，指定商品所属卖家",
        "required": true,
        "example": "SELLER789"
      },
      {
        "name": "startTime",
        "type": "string",
        "description": "预测开始时间，格式为年/月（如：2025/06），可选参数，默认为2025/06",
        "required": false,
        "example": "2025/06"
      },
      {
        "name": "endTime",
        "type": "string",
        "description": "预测结束时间，格式为年/月（如：2025/07），可选参数，默认为2025/07",
        "required": false,
        "example": "2025/07"
      }
    ],
    "outputSchema": {
      "prediction": "各时间段的预测销量和销售额，按月份组织的数据",
      "product": "商品详细信息",
      "query": "用于预测的参数"
    }
  },
  "toolName": "tool_details"
}
```

### 3. 执行工具

**请求**:
```
POST /api/mcp/execute
Content-Type: application/json

{
  "toolName": "predictSales",
  "parameters": {
    "productId": "P123456",
    "sellerId": "SELLER789",
    "startTime": "2025/06",
    "endTime": "2025/08"
  }
}
```

**响应**:
```json
{
  "status": "success",
  "data": {
    "prediction": {
      "2025/06": {
        "quantity": 45,
        "revenue": 4495.50
      },
      "2025/07": {
        "quantity": 52,
        "revenue": 5199.48
      },
      "2025/08": {
        "quantity": 48,
        "revenue": 4800.00
      }
    },
    "product": {
      "id": "P123456",
      "name": "Wireless Bluetooth Headphones",
      "category": "Electronics",
      "brand": "AudioTech",
      "price": 99.99,
      "description": "High-quality wireless headphones with noise cancellation"
    },
    "query": {
      "productId": "P123456",
      "sellerId": "SELLER789",
      "startTime": "2025/06",
      "endTime": "2025/08"
    }
  },
  "toolName": "predictSales"
}
```

## 可用工具详情

### 1. 销售排行分析 (topSellingProducts)

查询特定卖家在给定时间段内的销售排行榜，可按商品类别筛选。

**参数**:
- `sellerId` (必填): 卖家ID
- `startTime` (可选): 开始时间，格式为"年/月"，默认为"2025/01"
- `endTime` (可选): 结束时间，格式为"年/月"，默认为"2025/05"
- `category` (可选): 商品类别过滤
- `topN` (可选): 返回的排行数量，默认为3

**返回数据**:
- `products`: 销量排行商品列表，包含销售量和销售额等指标
- `count`: 返回的商品数量
- `query`: 使用的查询参数

### 2. 销量预测 (predictSales)

预测特定商品在未来时间段的销量，帮助卖家进行库存和营销决策。

**参数**:
- `productId` (必填): 商品ID
- `sellerId` (必填): 卖家ID
- `startTime` (可选): 预测开始时间，格式为"年/月"，默认为"2025/06"
- `endTime` (可选): 预测结束时间，格式为"年/月"，默认为"2025/07"

**返回数据**:
- `prediction`: 各时间段的预测销量和销售额
- `product`: 商品详细信息
- `query`: 用于预测的参数

### 3. 商品搜索 (searchProducts)

根据关键字和/或类别搜索商品。

**参数**:
- `keyword` (可选): 搜索关键字
- `category` (可选): 商品类别

**返回数据**:
- `results`: 搜索结果商品列表
- `count`: 结果数量
- `query`: 搜索参数

### 4. 相似商品搜索 (searchSimilarProducts)

基于文本描述或商品ID查找相似的商品，使用向量相似度检索技术。

**参数**:
- `description` (可选): 商品描述文本，用于查找与此描述相似的商品
- `productId` (可选): 商品ID，用于查找与此商品相似的其他商品
- `limit` (可选): 返回结果的数量限制，默认为3

**注**: `description` 和 `productId` 参数中必须至少提供一个。

**返回数据**:
- `products`: 相似商品列表
- `count`: 返回的商品数量

### 5. 订单列表查询 (getRecentOrders)

查询特定卖家的最近订单记录，支持分页功能。

**参数**:
- `sellerId` (必填): 卖家ID，用于查询特定卖家的订单
- `page` (可选): 页码，从0开始，默认为0（第一页）
- `size` (可选): 每页记录数，默认为20，最大为100

**返回数据**:
- `orders`: 订单列表
- `currentPage`: 当前页码
- `totalItems`: 总记录数
- `totalPages`: 总页数

### 6. 产品列表查询 (getProducts)

获取产品列表，支持按类别和卖家ID筛选。

**参数**:
- `category` (可选): 商品类别，用于按类别筛选产品
- `sellerId` (可选): 卖家ID，用于按卖家筛选产品

**返回数据**:
- `products`: 产品列表
- `count`: 产品数量
- `category`: 查询的类别（如果提供）
- `sellerId`: 查询的卖家ID（如果提供）

### 7. 产品详情查询 (getProductById)

根据产品ID获取产品详细信息。

**参数**:
- `id` (必填): 产品ID，用于查询特定产品的详细信息

**返回数据**:
- `product`: 产品详细信息

### 8. 产品管理 (manageProduct)

创建新产品或更新现有产品信息。

**参数**:
- `id` (更新时必填): 产品ID，更新产品时必填，创建新产品时不需要提供
- `name` (创建时必填): 产品名称
- `category` (创建时必填): 产品类别
- `brand` (创建时必填): 产品品牌
- `price` (创建时必填): 产品价格
- `description` (可选): 产品描述
- `sellerId` (创建时必填): 卖家ID

**返回数据**:
- `product`: 创建或更新后的产品信息
- `isNew`: 是否为新创建的产品
- `message`: 操作结果消息

## 集成示例

### Python示例

```python
import requests
import json

# MCP API基础URL
base_url = "http://localhost:8080/api/mcp"

# 1. 获取所有可用工具
response = requests.get(f"{base_url}/tools")
tools = response.json()
print("可用工具:", json.dumps(tools, indent=2, ensure_ascii=False))

# 2. 执行销量预测工具
prediction_request = {
    "toolName": "predictSales",
    "parameters": {
        "productId": "P123456",
        "sellerId": "SELLER789",
        "startTime": "2025/06",
        "endTime": "2025/08"
    }
}

response = requests.post(f"{base_url}/execute", json=prediction_request)
result = response.json()
print("预测结果:", json.dumps(result, indent=2, ensure_ascii=False))

# 3. 执行相似商品搜索
similar_products_request = {
    "toolName": "searchSimilarProducts",
    "parameters": {
        "description": "无线降噪耳机，支持蓝牙连接",
        "limit": 5
    }
}

response = requests.post(f"{base_url}/execute", json=similar_products_request)
result = response.json()
print("相似商品:", json.dumps(result, indent=2, ensure_ascii=False))

# 4. 获取产品详情
product_detail_request = {
    "toolName": "getProductById",
    "parameters": {
        "id": "P123456"
    }
}

response = requests.post(f"{base_url}/execute", json=product_detail_request)
result = response.json()
print("产品详情:", json.dumps(result, indent=2, ensure_ascii=False))

# 5. 创建新产品
create_product_request = {
    "toolName": "manageProduct",
    "parameters": {
        "name": "新一代智能手表",
        "category": "Wearables",
        "brand": "TechWear",
        "price": 299.99,
        "description": "智能手表带有健康监测功能，支持长达7天续航",
        "sellerId": "SELLER789"
    }
}

response = requests.post(f"{base_url}/execute", json=create_product_request)
result = response.json()
print("创建产品:", json.dumps(result, indent=2, ensure_ascii=False))
```

### JavaScript示例

```javascript
// MCP API基础URL
const baseUrl = 'http://localhost:8080/api/mcp';

// 1. 获取所有可用工具
async function getAvailableTools() {
  const response = await fetch(`${baseUrl}/tools`);
  const tools = await response.json();
  console.log('可用工具:', tools);
  return tools;
}

// 2. 执行销售排行分析工具
async function getTopSellingProducts() {
  const request = {
    toolName: 'topSellingProducts',
    parameters: {
      sellerId: 'SELLER789',
      startTime: '2025/01',
      endTime: '2025/05',
      category: 'Electronics',
      topN: 5
    }
  };
  
  const response = await fetch(`${baseUrl}/execute`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(request)
  });
  
  const result = await response.json();
  console.log('销售排行:', result);
  return result;
}

// 3. 执行订单列表查询
async function getSellerOrders() {
  const request = {
    toolName: 'getRecentOrders',
    parameters: {
      sellerId: 'SELLER789',
      page: 0,
      size: 10
    }
  };
  
  const response = await fetch(`${baseUrl}/execute`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(request)
  });
  
  const result = await response.json();
  console.log('订单列表:', result);
  return result;
}

// 4. 获取产品列表
async function getProductList() {
  const request = {
    toolName: 'getProducts',
    parameters: {
      category: 'Electronics'
    }
  };
  
  const response = await fetch(`${baseUrl}/execute`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(request)
  });
  
  const result = await response.json();
  console.log('产品列表:', result);
  return result;
}

// 5. 更新产品信息
async function updateProduct() {
  const request = {
    toolName: 'manageProduct',
    parameters: {
      id: 'P123456',
      price: 89.99,
      description: '升级版无线蓝牙耳机，支持主动降噪，续航时间延长'
    }
  };
  
  const response = await fetch(`${baseUrl}/execute`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(request)
  });
  
  const result = await response.json();
  console.log('更新产品:', result);
  return result;
}

// 执行示例
getAvailableTools()
  .then(() => getTopSellingProducts())
  .then(() => getSellerOrders())
  .then(() => getProductList())
  .then(() => updateProduct())
  .catch(err => console.error('Error:', err));
``` 