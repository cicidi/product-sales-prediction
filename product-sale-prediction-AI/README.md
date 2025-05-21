# API Chatbot with LangChain

A command-line chatbot that connects to API endpoints defined in an OpenAPI specification using LangChain.

## Features

- Parses OpenAPI (Swagger) YAML files to understand API endpoints
- Uses LangChain and OpenAI to create an intelligent chat interface
- Makes API calls on behalf of the user
- Provides colored command-line interface

## Setup

### Prerequisites

- Python 3.8 or higher
- An OpenAI API key

### Installation

1. Clone this repository
2. Install the required packages:

```bash
pip install -r requirements.txt
```

3. Create a `.env` file in the project root with the following content:

```
OPENAI_API_KEY=your_openai_api_key_here
API_SERVER_URL=http://localhost:8080
API_KEY=your_api_key_here  # Optional if your API requires authentication
```

## Usage

Run the chatbot:

```bash
python api_chatbot.py
```

You can now interact with the chatbot by typing natural language queries. The chatbot will:

1. Interpret your request
2. Determine which API endpoint to call
3. Make the API call
4. Present the results in a user-friendly format

Example commands:

- "Show me all products"
- "Find products in the Electronics category"
- "What endpoints are available in this API?"

Type 'exit' or 'quit' to exit the chatbot.

## Files

- `api_chatbot.py`: Main chatbot application
- `api_parser.py`: Utilities for parsing OpenAPI specifications
- `api_service.py`: Service for making API calls
- `requirements.txt`: Python dependencies 