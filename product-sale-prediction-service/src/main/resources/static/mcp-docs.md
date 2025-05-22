# Model-Centric Protocol (MCP) API Documentation

## Overview

Model-Centric Protocol (MCP) is a tool invocation interface provided for large language models (LLMs), enabling AI models to call various functions through a standardized protocol, such as product sales analysis and sales prediction.

This API design follows a tool-centric approach, modularizing system functions into independently callable "tools", allowing large models to flexibly combine and call them based on requirements.

## API Endpoints

MCP API provides the following main endpoints:

### 1. Get All Available Tools

**Request**:
```
GET /api/mcp/tools
```

**Response**:
```json
{
  "status": "success",
  "data": [
    {
      "name": "topSellingProducts",
      "displayName": "Sales Ranking Analysis",
      "description": "Analyze sales rankings for a specific seller within a given time period, supporting category filtering and querying top-selling products"
    },
    {
      "name": "predictSales",
      "displayName": "Sales Prediction",
      "description": "Predict sales volume and revenue for specific products in future time periods, helping sellers make better inventory and marketing decisions"
    },
    {
      "name": "searchProducts",
      "displayName": "Product Search",
      "description": "Search products by keywords and/or categories"
    },
    {
      "name": "searchSimilarProducts",
      "displayName": "Similar Product Search",
      "description": "Find similar products based on text description or product ID using vector similarity retrieval technology"
    },
    {
      "name": "getRecentOrders",
      "displayName": "Order List Query",
      "description": "Query recent order records for a specific seller, supporting pagination"
    },
    {
      "name": "getProducts",
      "displayName": "Product List Query",
      "description": "Get product list, supporting filtering by category and seller ID"
    },
    {
      "name": "getProductById",
      "displayName": "Product Detail Query",
      "description": "Get detailed product information by product ID"
    },
    {
      "name": "manageProduct",
      "displayName": "Product Management",
      "description": "Create new products or update existing product information"
    }
  ],
  "toolName": "list_tools"
}
```

### 2. Get Tool Details

**Request**:
```
GET /api/mcp/tools/{toolName}
```

Example:
```
GET /api/mcp/tools/predictSales
```

**Response**:
```json
{
  "status": "success",
  "data": {
    "name": "predictSales",
    "displayName": "Sales Prediction",
    "description": "Predict sales volume and revenue for specific products in future time periods, helping sellers make better inventory and marketing decisions",
    "parameters": [
      {
        "name": "productId",
        "type": "string",
        "description": "Product ID, required parameter, specifies the product for sales prediction",
        "required": true,
        "example": "P123456"
      },
      {
        "name": "sellerId",
        "type": "string",
        "description": "Seller ID, required parameter, specifies the product owner",
        "required": true,
        "example": "SELLER789"
      },
      {
        "name": "startTime",
        "type": "string",
        "description": "Prediction start time, format as year/month (e.g., 2025/06), optional parameter, defaults to 2025/06",
        "required": false,
        "example": "2025/06"
      },
      {
        "name": "endTime",
        "type": "string",
        "description": "Prediction end time, format as year/month (e.g., 2025/07), optional parameter, defaults to 2025/07",
        "required": false,
        "example": "2025/07"
      }
    ],
    "outputSchema": {
      "prediction": "Predicted sales volume and revenue for each time period, organized by month",
      "product": "Detailed product information",
      "query": "Parameters used for prediction"
    }
  },
  "toolName": "tool_details"
}
```

### 3. Execute Tool

**Request**:
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

**Response**:
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

## Available Tool Details

### 1. Sales Ranking Analysis (topSellingProducts)

Query sales rankings for a specific seller within a given time period, with optional category filtering.

**Parameters**:
- `sellerId` (required): Seller ID
- `startTime` (optional): Start time, format as "year/month", defaults to "2025/01"
- `endTime` (optional): End time, format as "year/month", defaults to "2025/05"
- `category` (optional): Product category filter
- `topN` (optional): Number of rankings to return, defaults to 3

**Return Data**:
- `products`: List of products by sales ranking, including sales volume and revenue metrics
- `count`: Number of products returned
- `query`: Query parameters used

### 2. Sales Prediction (predictSales)

Predict sales volume for specific products in future time periods to help sellers make inventory and marketing decisions.

**Parameters**:
- `productId` (required): Product ID
- `sellerId` (required): Seller ID
- `startTime` (optional): Prediction start time, format as "year/month", defaults to "2025/06"
- `endTime` (optional): Prediction end time, format as "year/month", defaults to "2025/07"

**Return Data**:
- `prediction`: Predicted sales volume and revenue for each time period
- `product`: Detailed product information
- `query`: Parameters used for prediction

### 3. Product Search (searchProducts)

Search products by keywords and/or categories.

**Parameters**:
- `keyword` (optional): Search keyword
- `category` (optional): Product category

**Return Data**:
- `results`: List of search result products
- `count`: Number of results
- `query`: Search parameters

### 4. Similar Product Search (searchSimilarProducts)

Find similar products based on text description or product ID using vector similarity retrieval technology.

**Parameters**:
- `description` (optional): Product description text for finding similar products
- `productId` (optional): Product ID for finding similar products
- `limit` (optional): Limit on number of results to return, defaults to 3

**Note**: At least one of `description` or `productId` parameters must be provided.

**Return Data**:
- `products`: List of similar products
- `count`: Number of products returned

### 5. Order List Query (getRecentOrders)

Query recent order records for a specific seller, supporting pagination.

**Parameters**:
- `sellerId` (required): Seller ID for querying specific seller's orders
- `page` (optional): Page number, starting from 0, defaults to 0 (first page)
- `size` (optional): Records per page, defaults to 20, maximum 100

**Return Data**:
- `orders`: Order list
- `currentPage`: Current page number
- `totalItems`: Total number of records
- `totalPages`: Total number of pages

### 6. Product List Query (getProducts)

Get product list, supporting filtering by category and seller ID.

**Parameters**:
- `category` (optional): Product category for filtering products
- `sellerId` (optional): Seller ID for filtering products

**Return Data**:
- `products`: Product list
- `count`: Number of products
- `category`: Queried category (if provided)
- `sellerId`: Queried seller ID (if provided)

### 7. Product Detail Query (getProductById)

Get detailed product information by product ID.

**Parameters**:
- `id` (required): Product ID for querying specific product details

**Return Data**:
- `product`: Detailed product information

### 8. Product Management (manageProduct)

Create new products or update existing product information.

**Parameters**:
- `id` (required for updates): Product ID, required when updating products, not needed for creating new products
- `name` (required for creation): Product name
- `category` (required for creation): Product category
- `brand` (required for creation): Product brand
- `price` (required for creation): Product price
- `description` (optional): Product description
- `sellerId` (required for creation): Seller ID

**Return Data**:
- `product`: Created or updated product information
- `isNew`: Whether it's a newly created product
- `message`: Operation result message

## Integration Examples

### Python Example

```python
import requests
import json

# MCP API base URL
base_url = "http://localhost:8080/api/mcp"

# 1. Get all available tools
response = requests.get(f"{base_url}/tools")
tools = response.json()
print("Available tools:", json.dumps(tools, indent=2, ensure_ascii=False))

# 2. Execute sales prediction tool
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
print("Prediction results:", json.dumps(result, indent=2, ensure_ascii=False))

# 3. Execute similar product search
similar_products_request = {
    "toolName": "searchSimilarProducts",
    "parameters": {
        "description": "Wireless noise-cancelling headphones with Bluetooth connectivity",
        "limit": 5
    }
}

response = requests.post(f"{base_url}/execute", json=similar_products_request)
result = response.json()
print("Similar products:", json.dumps(result, indent=2, ensure_ascii=False))

# 4. Get product details
product_detail_request = {
    "toolName": "getProductById",
    "parameters": {
        "id": "P123456"
    }
}

response = requests.post(f"{base_url}/execute", json=product_detail_request)
result = response.json()
print("Product details:", json.dumps(result, indent=2, ensure_ascii=False))

# 5. Create new product
create_product_request = {
    "toolName": "manageProduct",
    "parameters": {
        "name": "Next-Gen Smart Watch",
        "category": "Wearables",
        "brand": "TechWear",
        "price": 299.99,
        "description": "Smart watch with health monitoring features and 7-day battery life",
        "sellerId": "SELLER789"
    }
}

response = requests.post(f"{base_url}/execute", json=create_product_request)
result = response.json()
print("Create product:", json.dumps(result, indent=2, ensure_ascii=False))
```

### JavaScript Example

```javascript
// MCP API base URL
const baseUrl = 'http://localhost:8080/api/mcp';

// 1. Get all available tools
async function getAvailableTools() {
  const response = await fetch(`${baseUrl}/tools`);
  const tools = await response.json();
  console.log('Available tools:', tools);
  return tools;
}

// 2. Execute sales ranking analysis tool
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
  console.log('Sales ranking:', result);
  return result;
}

// 3. Execute order list query
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
  console.log('Order list:', result);
  return result;
}

// 4. Get product list
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
  console.log('Product list:', result);
  return result;
}

// 5. Update product information
async function updateProduct() {
  const request = {
    toolName: 'manageProduct',
    parameters: {
      id: 'P123456',
      price: 89.99,
      description: 'Upgraded wireless Bluetooth headphones with active noise cancellation and extended battery life'
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
  console.log('Update product:', result);
  return result;
}

// Execute examples
getAvailableTools()
  .then(() => getTopSellingProducts())
  .then(() => getSellerOrders())
  .then(() => getProductList())
  .then(() => updateProduct())
  .catch(err => console.error('Error:', err));
``` 