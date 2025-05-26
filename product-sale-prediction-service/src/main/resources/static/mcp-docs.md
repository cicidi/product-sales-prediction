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
      "name": "predict_by_category",
      "displayName": "Predict by Category",
      "description": "Predict future Top/Best N sell products within a specific category."
    },
    {
      "name": "predict_by_product_id",
      "displayName": "Predict by Product ID",
      "description": "Predict future sales for a specific product by product ID."
    },
    {
      "name": "list_orders",
      "displayName": "Order List Query",
      "description": "Query order records for a specific seller, supporting time range and pagination."
    },
    {
      "name": "list_products",
      "displayName": "Product List",
      "description": "List products with optional filtering by category and seller ID."
    },
    {
      "name": "manage_product",
      "displayName": "Product Management",
      "description": "Create, update, or delete product information."
    },
    {
      "name": "get_product_detail",
      "displayName": "Product Detail",
      "description": "Get detailed information about a specific product."
    },
    {
      "name": "analyze_sales",
      "displayName": "Sales Analytics",
      "description": "Get daily product sales summary and total summary for a time range."
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
GET /api/mcp/tools/predict_by_category
```

**Response**:
```json
{
  "status": "success",
  "data": {
    "name": "predict_by_category",
    "displayName": "Predict by Category",
    "description": "Predict future Top/Best N sell products within a specific category.",
    "parameters": [
      {
        "name": "category",
        "type": "string",
        "description": "Category, required parameter, specifies the category for sales prediction",
        "required": true,
        "example": "electronics"
      },
      {
        "name": "seller_id",
        "type": "string",
        "description": "Seller ID, required parameter, specifies the seller for sales prediction",
        "required": true,
        "example": "seller_1"
      },
      {
        "name": "top_n",
        "type": "integer",
        "description": "Number of top products to predict (Required)",
        "required": true,
        "example": 10
      },
      {
        "name": "start_date",
        "type": "string",
        "description": "Start date for prediction, format yyyy/MM/dd (e.g., 2025/06/01), required parameter",
        "required": true,
        "example": "2025/06/01"
      },
      {
        "name": "end_date",
        "type": "string",
        "description": "End date for prediction, format yyyy/MM/dd (e.g., 2025/06/01), optional parameter, if not provided will only predict one day",
        "required": false,
        "example": "2025/06/01"
      }
    ],
    "outputSchema": {
      "predicationList": "List of daily predictions",
      "startDate": "Prediction start date",
      "endDate": "Prediction end date",
      "totalQuantity": "Total predicted sales quantity",
      "totalDays": "Total number of days predicted"
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
  "toolName": "predict_by_category",
  "parameters": {
    "category": "electronics",
    "seller_id": "seller_1",
    "top_n": 10,
    "start_date": "2025/06/01",
    "end_date": "2025/06/01"
  }
}
```

**Response**:
```json
{
  "status": "success",
  "data": {
    "predicationList": [
      {
        "date": "2025/06/01",
        "productId": "p100",
        "quantity": 50
      }
    ],
    "startDate": "2025/06/01",
    "endDate": "2025/06/01",
    "totalQuantity": 50,
    "totalDays": 1
  },
  "toolName": "predict_by_category"
}
```

## Available Tool Details

### 1. Predict by Category (predict_by_category)

Predict future Top/Best N sell products within a specific category.

**Parameters**:
- `category` (required): Category, specifies the category for sales prediction. Example: "electronics"
- `seller_id` (required): Seller ID, specifies the seller for sales prediction. Example: "seller_1"
- `top_n` (required): Number of top products to predict. Example: 10
- `start_date` (required): Start date for prediction, format yyyy/MM/dd. Example: "2025/06/01"
- `end_date` (optional): End date for prediction, format yyyy/MM/dd. If not provided, will only predict one day. Example: "2025/06/01"

**Return Data**:
- `predicationList`: List of daily predictions
- `startDate`: Prediction start date
- `endDate`: Prediction end date
- `totalQuantity`: Total predicted sales quantity
- `totalDays`: Total number of days predicted

### 2. Predict by Product ID (predict_by_product_id)

Predict future sales for a specific product by product ID.

**Parameters**:
- `product_id` (required): Product ID, specifies the product for sales prediction. Example: "p100"
- `seller_id` (required): Seller ID, specifies the seller for sales prediction. Example: "seller_1"
- `sale_price` (optional): Sale price, if not provided will use original price. Example: 99.99
- `start_date` (required): Start date for prediction, format yyyy/MM/dd. Example: "2025/06/01"
- `end_date` (optional): End date for prediction, format yyyy/MM/dd. If not provided, will only predict one day. Example: "2025/06/01"

**Return Data**:
- `predicationList`: List of daily predictions
- `startDate`: Prediction start date
- `endDate`: Prediction end date
- `totalQuantity`: Total predicted sales quantity
- `totalDays`: Total number of days predicted

### 3. Order List Query (list_orders)

Query order records for a specific seller, supporting time range and pagination.

**Parameters**:
- `seller_id` (required): Seller ID, sellers cannot view other sellers' orders. Example: "seller_1"
- `start_time` (optional): Start time, format yyyy/MM/dd. Example: "2025/05/01"
- `end_time` (optional): End time, format yyyy/MM/dd. Example: "2025/05/01"
- `page` (optional): Page number, starting from 0. Example: 0
- `size` (optional): Records per page, maximum 100. Example: 20

**Return Data**:
- `orders`: Order list
- `current_page`: Current page number
- `total_items`: Total number of records
- `total_pages`: Total number of pages
- `start_time`: Query start time
- `end_time`: Query end time

### 4. Product List (list_products)

List products with optional filtering by category and seller ID.

**Parameters**:
- `category` (optional): Product category, used to filter products by category. Example: "electronics"
- `seller_id` (optional): Seller ID, used to filter products by seller. Example: "seller_1"
- `page` (optional): Page number, starting from 0. Example: 0
- `size` (optional): Records per page, maximum 100. Example: 20

**Return Data**:
- `products`: Product list
- `total_count`: Total number of products
- `current_page`: Current page number
- `total_pages`: Total number of pages
- `category`: Queried category (if provided)
- `seller_id`: Queried seller ID (if provided)

### 5. Product Management (manage_product)

Create, update, or delete product information.

**Parameters**:
- `product_id` (required for updates): Product ID. Example: "p100"
- `seller_id` (required for creation): Seller ID. Example: "seller_1"
- `name` (required for creation): Product name. Example: "Wireless Bluetooth Earphones"
- `category` (required for creation): Product category. Example: "Electronics"
- `brand` (required for creation): Brand. Example: "Sony"
- `price` (required for creation): Price. Example: 999.99

**Return Data**:
- `product_id`: Product ID
- `seller_id`: Seller ID
- `name`: Product name
- `category`: Product category
- `brand`: Brand
- `price`: Price
- `created_at`: Creation time
- `updated_at`: Update time

### 6. Product Detail (get_product_detail)

Get detailed information about a specific product.

**Parameters**:
- `product_id` (required): Product ID, used to query specific product details. Example: "p100"

**Return Data**:
- `product_id`: Product ID
- `seller_id`: Seller ID
- `name`: Product name
- `category`: Product category
- `brand`: Brand
- `price`: Price
- `created_at`: Creation time
- `updated_at`: Update time

### 7. Sales Analytics (analyze_sales)

Get daily product sales summary and total summary for a time range.

**Parameters**:
- `seller_id` (optional): Seller ID to filter sales. Example: "seller_1"
- `product_id` (optional): Product ID to filter sales. Example: "p100"
- `start_time` (required): Start time, format yyyy/MM/dd. Example: "2025/05/01"
- `end_time` (optional): End time, format yyyy/MM/dd. Example: "2025/05/01"
- `category` (optional): Category to filter by. Example: "electronics"
- `top_n` (optional): Number of top products to return. Example: 10

**Return Data**:
- `dailyProductSales`: List of daily product sales with productId, quantity, date, and revenue
- `totalSummary`: List of total product sales summary with productId, quantity, date='total', and revenue
- `startTime`: Start time of analysis period
- `endTime`: End time of analysis period
- `sellerId`: Seller ID if filtered
- `productId`: Product ID if filtered
- `category`: Category if filtered
- `topN`: Top N filter if applied

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
    "toolName": "predict_by_category",
    "parameters": {
        "category": "electronics",
        "seller_id": "seller_1",
        "top_n": 10,
        "start_date": "2025/06/01",
        "end_date": "2025/06/01"
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
    "toolName": "get_product_detail",
    "parameters": {
        "product_id": "p100"
    }
}

response = requests.post(f"{base_url}/execute", json=product_detail_request)
result = response.json()
print("Product details:", json.dumps(result, indent=2, ensure_ascii=False))

# 5. Create new product
create_product_request = {
    "toolName": "manage_product",
    "parameters": {
        "name": "Next-Gen Smart Watch",
        "category": "Wearables",
        "brand": "TechWear",
        "price": 299.99,
        "description": "Smart watch with health monitoring features and 7-day battery life",
        "seller_id": "seller_1"
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
      sellerId: 'seller_1',
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
      sellerId: 'seller_1',
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
      id: 'p100',
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