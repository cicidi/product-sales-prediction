# Product Sales Prediction System Design

---

## A. Requirements Definition

### ✅ Problem Statement

Design a system to predict **“What will be the top-selling products by category for a given seller
and time window?”** in QuickBooks Commerce.

### ✅ High-Level Requirements

- Predict top-N selling products using AI/ML based on historical and contextual signals
- Provide a user-friendly dashboard and chatbot for seller-side access
- Ensure system scalability, reliability, observability

### ✅ Goals Breakdown

#### 📝 Business Objectives

- **Inputs:**
    - Seller ID
    - Product Category
    - Time Range (e.g. next week/month/year)
    - Top-N value

- **Output:**
    - Ranked list of predicted top-selling products with estimated quantities

- **User Scenario:**  
  A seller wants to know what products will perform best next week in “Electronics” category. They
  log into QuickBooks, input category and time range, and get predictions to support marketing
  decisions.

- **Service Scale:**
    - ~10,000 sellers × 1,000 daily orders → 10M orders/day
    - ~100 requests/sec at peak (holiday seasons)
    - 10k seller, every use has 10 request per day, 1 requests per second.

- **Data Sources:**
    - Historical order data
    - Product metadata
    - Seller-product relationships

#### 📌 AI Application Points

- Supervised regression or time-series forecasting models for product-level sales
- LLM + Agent for natural-language UX & Traditional dynamic dashboard queries

---

## B. Predictive System High Level Architecture

<img src="documents/Predication-Page-1.jpg" alt="Description" width="960"/>

## C. Machine Learning Platform Low Level Architecture

### C.1 Feature Engineering

| Feature                    | Description                            |
|----------------------------|----------------------------------------|
| `product_id`               | Unique ID for product                  |
| `seller_id`                | Seller who sold the product            |
| `sale_price`               | Final transaction price                |
| `original_price`           | Price before discount                  |
| `is_holiday`               | Whether the sale happened on a holiday |
| `is_weekend`               | Weekend indicator (Saturday/Sunday)    |
| `day_of_week`              | Day of week (0–6)                      |
| `day_of_month`             | Day of month (1–31)                    |
| `month`                    | Month (1–12)                           |
| `lag_1`, `lag_7`, `lag_30` | Aggregated past sales quantities       |

### C.2 Candidate Models

- **Tree-based models (XGBoost, LightGBM)**
    - Good for structured, categorical + numeric data

### C.3 Data Simulation

📈 Quantity Simulation Logic

`quantity` is a function of several features:

| Feature                          | Effect on Quantity                                      |
|----------------------------------|---------------------------------------------------------|
| `sale_price` vs `original_price` | Greater discount → higher quantity (up to 20% discount) |
| `is_holiday`                     | Boosts electronics and clothes sales                    |
| `is_weekend`                     | Boosts food sales significantly                         |
| `product_id`                     | Different base rates, all balanced                      |
| `seller_id`                      | Large sellers have slightly higher volume               |
| `day_of_week`                    | Captures weekday/weekend patterns                       |
| `day_of_month` / `month`         | Reflect seasonal/monthly variations                     |

[Simulation Requirment](./product-sale-prediction-AI/generate/sales_data_specification.md) / [generate_test_data.py](product-sale-prediction-AI/generate/generate_test_data.py)

### C.4 Training & Tuning

1. Data aggregation  (daily quantity per product per
   seller)   [prepare_sales_train_data.py](product-sale-prediction-AI/train/prepare_sales_train_data.py)
   other options:
    - **Sales Aggregator** — Kafka + Spark-based data preprocessing pipeline
    - **Batch ETL** — Periodic aggregation jobs using Airflow or similar tools
    - **Data Lake** — Store raw logs in S3 or HDFS for future analysis
2. Feature generation(lags, date features,
   flags)   [prepare_sales_train_data.py](product-sale-prediction-AI/train/prepare_sales_train_data.py)
3. Evaluation: Quantity Precision vs Real
   data.  [evaludate_model.py](product-sale-prediction-AI/evaluate/evaludate_model.py)  
   <img src="documents/Predication%20vs%20Real%20Data.png" alt="Description" width="600"/>

### C.5 Evaluation Metrics

- **MAE [evaludate_model.py](product-sale-prediction-AI/evaluate/evaludate_model.py) (Mean Absolute
  Error)**
- **RMSE (Root Mean Square Error)**
- **Precision@N / Recall@N** for top-N product list  
  <img src="documents/Model Evaluation.png" alt="Description" width="600"/>

### C.6 Deployment & Integration

- **API Endpoint:**
  `POST /predict` [doc] (http://localhost:8000/docs#/default/predict_single_predict_post)
- **Input JSON:**
  ```json
  {
  "product_id": "p101",
  "seller_id": "seller_2",
  "sale_price": 899.99,
  "original_price": 1099,
  "is_holiday": 0,
  "is_weekend": 1,
  "day_of_week": 6,
  "day_of_month": 15,
  "month": 5,
  "lag_1": 120,
  "lag_7": 89,
  "lag_30": 95
  }
  ```  
- **Output JSON:**
  ```json
  {
  "predicted_quantity": 593.7947998046875,
  "status": "success"
  }
  ```  

- **Machine Learning Deployment Options:**  
  FastAPI [sales_prediction_api.py](product-sale-prediction-AI/sales_prediction_api.py), Spring Boot
  or AWS SageMaker.

      ![python_predication_api.png](documents/python_predication_api.png)

---

## C. Predication Backend Microservices(Orchestration and Integration)

### C.1 Service Function

- **Access control** — Check if seller able to access API resource, and seller can only access their
  own products.
- **Integrate Other Services**
    - **Integrate Prediction Model Service** — Call `/predict` endpoint with product features
    - **Seller Context Service** — Fetch seller metadata (e.g. seller_id, seller is selling which
      products, etc.)
    - **Product Details Service** — Fetch product descriptions, category, etc.
    - **Sales History Service** — Retrieve historical sales data for feature generation
- **Model Context Protocol** — Standardized API for model interactions, allowing easy swapping of
- **Restful API** — Use RESTful endpoints for model predictions, allowing easy integration with
- **Caching**: **Cache recent queries & deduplicated predictions

### C.2 RESTful API Design

Swagger Doc http://localhost:8080/swagger-ui/index.html#

| name             | Endpoint           | Description                                                                                                                                                                      |
|------------------|--------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `Products API`   | /products /product | Product management endpoints.  list product, search prodcut by productid and/or category and/or seler_id                                                                         |
| `Orders API`     | /orders?sellerId=  | Retrieve orders with optional filters for seller ID, product ID, category, date range. Results are sorted by timestamp in descending order. Default returns last 30 days orders. |
| `Predicate API`  | /salles/predicate  | Predict future sales for a specific product using historical data and ML model                                                                                                   |
| `Analystics API` | /sales/analystics  | Get daily product sales summary and total summary for a time range. If topN is provided, returns only top N products by total sales.                                             |

### C.2  Model Context Protocol

Model-Centric Protocol (MCP) is a tool invocation interface provided for large language models (
LLMs), enabling AI models to call various functions through a standardized protocol, such as product
sales analysis and sales prediction.
This API design follows a tool-centric approach, modularizing system functions into independently
callable "tools", allowing large models to flexibly combine and call them based on requirements.
[mcp-docs.md](product-sale-prediction-service/src/main/resources/static/mcp-docs.md)

| Endpoint                | Description                                                                      |
|-------------------------|----------------------------------------------------------------------------------|
| `predict_by_category`   | Predict future Top/Best N sell products within a specific category.              |
| `predict_by_product_id` | Predict future sales for a specific product by product ID.                       |
| `list_orders`           | Query order records for a specific seller, supporting time range and pagination. |
| `list_products`         | List products with optional filtering by category and seller ID..                |
| `manage_product`        | Get detailed information about a specific product.                               |
| `analyze_sales`         | Get daily product sales summary and total summary for a time range.              |

Sample Question Supported:

``` 
- List all orders for seller_3 in the last month.
- Predict seller_1's future sales for product p100 in next 10 days.
- What are the details of product ID p101?
- Predict the seller 2 's top 3 selling electronics next week.
- List all products for seller_5.
- Update the price of product ID p300 to 135.0.
- What is the total revenue for seller_3 in the last quarter?
- How many units of product ID p200 were sold by seller_1 last month?
- What are the top 5 best-selling products this year?
- What is the average order value for seller_4?
```

---

## D. Agent + LLM + MCP Integration

LLM agents can be used to provide a natural language interface for querying product sales
predictions.
Functionalities include:

- **Natural Language Understanding**: Parse user queries to identify intent and parameters
- **Tool Invocation**: Call appropriate MCP endpoints based on parsed intent
- **Context Management**: Maintain conversation state and context for follow-up questions
- **Error Handling**: Gracefully handle invalid inputs or API errors
- **Response Generation**: Format API responses into natural language summaries

### D.1 Chatbot Workflow

User types:
> “What’s my best-performing product next week in electronics?”

Agent thinking Steps:

1. What is the Question ask me to do? -> predict_by_category
2. What is the input? -> { "seller_id": "seller_1", "category": "electronics", "time_range": "next
   week", "top_n": 5 }
3. "Next Week" is not validate input -> "What is the time range?
4. Call "convert_time_range" tool to convert "next week" to a valid time range.
5. Seller ID and category are not provided -> ask user "What is your seller"
6. Fills in missing info (e.g. `seller_id`, `category`)
7. Calls `/mcp/sales/predict`
8. Returns conversational summary with top-N products and explanations
9. If user next more details on product, call `/mcp/sales/manage_product` to get product details

### D.2 LLM API usage

As LLM as high cost and long latency, we need a monitoring to track the usage and performance of LLM
API calls.
By learning from the usage patterns, we can optimize the LLM calls to reduce cost and improve
performance in the following ways:

- **Rate Limiting**: Throttle LLM calls to avoid overuse
- **Chunking**: Understand and Break down large queries into smaller parts to reduce token usage
- **Caching**: Cache common queries and responses to reduce redundant LLM calls

https://smith.langchain.com/o/ae357598-8cdb-481a-96a8-c2db51f867d5/dashboards/projects/f59f2388-4243-4c1f-9cc8-a6c345592242
![langsmith2.png](documents/langsmith2.png)
![langsmith.png](documents/langsmith.png)

## E. Web UI and Dashboard

Even we have a chatbot interface, we still need a web UI to provide a more visual and interactive.
dashboard for sellers to view and filter predictions.

http://localhost:4200/dashboard
![dashboard.png](documents/dashboard.png)

## F. Identity and Access Management (not implemented yet)

To ensure secure access to the prediction APIs, we need implement an Identity and Access
Management (IAM)

- **Roles and Permissions**: system to control who can access which resources.
- **ClientID** and **ClientSecret**: are used to authenticate the client application, and return *
  *AccessToken**
- **AccessToken**: is a temporary JWT token that contains the user's identity and permissions.

## F. Cost Optimization / Performance & Latency / Accuracy

- LLM agent Cost and performance Optimization
    - User Rag to store and retrieve common queries
    - User Rag to store and retrieve chat history and context , currently use entire chat history,
      but can
      be optimized to only store the last 10 messages.
    - Different model for different user, e.g. use a smaller model for low-traffic sellers. or
      smaller model for convert time range.
    - Use open-source LLMs (e.g. Llama 2, Falcon) for cost-sensitive applications
    - User prompt engineering to reduce token usage, reject irrelevant queries, and avoid
      unnecessary LLM calls, reply message in short and concise.

- Web Service Cost/Latency Optimization
    - Dynamic scaling of microservices based on traffic
    - User Caching.

- Database Cost/Latency Optimization
    - Use Redis for caching frequently accessed data (e.g. product details, seller metadata)
    - Use database indexing for faster query performance
    - Use partitioning to scale database horizontally
    - Use read replicas to distribute read traffic
- Machine Learning Model accuracy
    - Use XGBoost or LightGBM for structured data
    - Use feature engineering to improve model performance
    - Use hyperparameter tuning to optimize model performance
    - Use cross-validation to evaluate model performance
    - Use SHAP values to explain model predictions
    - Early Stopping
    - Online Learning / Active Learning


- Use historical grouping to reduce compute cost for low-traffic sellers

## F. Tech Stack

1. `product-sale-prediction-ML`: Simulated data, XGBoost model training, deployment
2. `product-sale-prediction-service`: SpringBoot + Postgres + REST/MCP backend with scalable APIs
3. `quickbooks-sales-dashboard`: Angular app for filtering and visualizing predictions
4. `ai_chat_bot`: LangChain + MCP + LangSmith + Streamlit to query forecasts via chat
5. `Monitoring tools`: Datadog / Prometheus for latency, Langsmith.

---
---

## H. Summary

This system helps sellers make data-driven decisions by forecasting top-selling products in upcoming
time windows. It combines:

- Practical AI integration
- Real-time, explainable outputs
- Scalable microservices and chat interfaces
- Monitoring and fallback strategies for robustness
