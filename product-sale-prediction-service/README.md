# Product Predictor Service

An intelligent e-commerce prediction system built with Spring Boot, designed to be easily accessible via REST API and LLM tools through Model-Centric Protocol (MCP).

## Features

- Product management with vector similarity search
- Order history tracking
- Sales analytics and top product identification
- Sales prediction using ML models with both Java native and Python integration
- LLM-friendly API endpoints through MCP

## Technology Stack

- Java 17
- Spring Boot 3.1.0
- Spring Data JPA
- H2 Database (in-memory)
- Qdrant Vector Database for semantic search
- OpenAPI / Swagger documentation
- JPMML for native Java ML model integration
- AWS SageMaker integration

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven

### Running the Application

1. Clone the repository:

```bash
git clone https://github.com/yourusername/product-predictor-service.git
cd product-predictor-service
```

2. Run the application using Maven:

```bash
mvn spring-boot:run
```

The application will start on port 8080.

### CSV Data Loading

By default, the application loads sample product and sales data from CSV files in the resources directory:
- `src/main/resources/final_sample_products.csv`
- `src/main/resources/sales_2023_2025_realistic.csv`

You can replace these files with your own data following the same format.

## API Endpoints

### Products API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET    | `/v1/products` | Get all products, with optional category and sellerId filters |
| GET    | `/v1/product/{id}` | Get product by ID |
| POST   | `/v1/product/search-similar` | Search for similar products by ID or description |

### Orders API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET    | `/v1/orders` | Get recent orders with pagination |

### Sales Analytics API

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST   | `/v1/sales/search` | Find top-selling products by criteria |
| POST   | `/v1/sales/predict` | Predict future sales for a specific product |

## MCP Endpoints for LLMs

The following endpoints are designed for LLM agents (like LangChain tools):

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST   | `/v1/mcp/search-similar-products` | Find similar products |
| POST   | `/v1/mcp/get-recent-orders` | Get recent orders |
| POST   | `/v1/sales/mcp/search-top-products` | Find top-selling products |
| POST   | `/v1/sales/mcp/predict-sales` | Predict future sales |

## API Documentation

Swagger UI is available at:
```
http://localhost:8080/swagger-ui.html
```

OpenAPI specification is available at:
```
http://localhost:8080/api-docs
```

## Configuration

Key configuration in `application.properties`:

- `server.port`: Application port
- `csv.product-file`: Path to product CSV file
- `csv.sales-file`: Path to sales CSV file
- Qdrant Vector Database settings
- AWS SageMaker settings
- OpenAI API settings (for embeddings)

### Qdrant Vector Database

The application uses Qdrant for storing and searching vector embeddings. Configuration parameters:

```properties
qdrant.host=bcdb7803-3764-46a7-9d0e-d115a81f8ed9.europe-west3-0.gcp.cloud.qdrant.io
qdrant.port=6333
qdrant.api-key=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhY2Nlc3MiOiJtIn0.AIQURDWwgjJ3gpl6lZ_ppDz_m6kYK8nan--LVVPubPo
qdrant.collection.name=product_embeddings
qdrant.embedding.dimension=1536
```

## Sales Prediction ML Model Integration

The application can predict product sales through two methods:

### 1. Java Native Integration (Recommended)

The service supports direct loading of ML models in PMML format for in-JVM prediction:

1. Convert the XGBoost model to PMML format:
```bash
chmod +x src/main/resources/scripts/convert_model.sh
./src/main/resources/scripts/convert_model.sh
```

2. The model will be loaded automatically when the Spring Boot application starts.

3. Use the endpoint to make predictions:
```
POST /v1/sales/predict-ml
```

### 2. Python Server Integration (Alternative)

Alternatively, you can run the Python Flask server alongside the Java application:

1. Start the Python prediction server:
```bash
chmod +x src/main/resources/scripts/run_model_server.sh
./src/main/resources/scripts/run_model_server.sh
```

2. The Spring Boot application will call the Python API endpoint for predictions.

## Configuration

Key configuration in `application.properties`:

- `server.port`: Application port
- `csv.product-file`: Path to product CSV file
- `csv.sales-file`: Path to sales CSV file
- Qdrant Vector Database settings
- AWS SageMaker settings
- OpenAI API settings (for embeddings)

### ML Model Configuration

```properties
# Path to the ML model directory
ml.model.base.path=../product-sale-prediction-AI/train/model
# Whether to use the local model or mock predictions
ml.model.use.local.model=true
# API endpoint for the Python model server
ml.model.api.endpoint=http://localhost:8000/predict
```

## MCP Integration for LLM Agents

To use this API with LangChain or other LLM frameworks, use the MCP-compatible endpoints:

```python
from langchain.agents import Tool

tools = [
    Tool(
        name="search_similar_products",
        description="Find products similar to a description or product ID",
        func=lambda query: requests.post(
            "http://localhost:8080/v1/mcp/search-similar-products",
            json={"description": query}
        ).json()
    ),
    Tool(
        name="predict_product_sales",
        description="Predict sales for a product",
        func=lambda params: requests.post(
            "http://localhost:8080/v1/sales/mcp/predict-sales",
            json=params
        ).json()
    )
]
```

## License

This project is licensed under the Apache 2.0 License - see the LICENSE file for details. 