# AI Sales Assistant

An intelligent sales assistant built with LangChain and Streamlit that dynamically loads tools from a Swagger API to help sellers analyze data, make predictions, and visualize sales information.

## Features

- 🤖 Dynamic tool loading from Swagger/OpenAPI specification
- 💬 Natural language interface for querying sales data
- 📊 Automatic data visualization with charts
- 🔄 Context-aware conversations
- 🛠️ Extensible tool system
- 📝 Chat history persistence
- 🎨 Modern, responsive UI

## Prerequisites

- Python 3.8+
- OpenAI API key
- Running backend server with Model-Centric Protocol endpoints

## Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd product-sale-prediction-AI
```

2. Create and activate a virtual environment:
```bash
python -m venv .venv
source .venv/bin/activate  # On Windows: .venv\Scripts\activate
```

3. Install dependencies:
```bash
pip install -r requirements.txt
```

4. Create a `.env` file in the project root and add your OpenAI API key:
```
OPENAI_API_KEY=your_api_key_here
```

## Usage

1. Ensure your backend server is running on `http://localhost:8080`

2. Start the Streamlit application:
```bash
streamlit run chat_sales_assistant.py
```

3. Open your browser and navigate to the URL shown in the terminal (usually `http://localhost:8501`)

## Project Structure

```
.
├── README.md
├── requirements.txt
├── .env
├── chat_sales_assistant.py    # Main Streamlit application
└── tools/
    └── mcp_tool_loader.py     # Dynamic tool loader for MCP endpoints
```

## Features

### Dynamic Tool Loading
- Automatically loads tools from Swagger/OpenAPI specification
- Filters for "Model-Centric Protocol" tagged endpoints
- Handles both GET and POST requests
- Supports path parameters, query parameters, and request bodies

### Chat Interface
- Natural language interaction
- Context-aware responses
- Chat history persistence
- Error handling and debug information

### Data Visualization
- Automatic chart generation based on data type
- Support for various chart types (bar, line, etc.)
- Summary statistics display
- Raw data viewing option

## Development

To add new features or modify existing ones:

1. Tool Loader (`tools/mcp_tool_loader.py`):
   - Handles Swagger spec parsing
   - Creates LangChain tools
   - Manages API interactions

2. Main App (`chat_sales_assistant.py`):
   - Streamlit UI components
   - LangChain agent configuration
   - Visualization logic
   - Session state management

## Troubleshooting

1. **API Connection Issues**
   - Ensure the backend server is running
   - Check the base URL configuration
   - Verify the API endpoints are accessible

2. **OpenAI API Issues**
   - Verify your API key is correctly set in `.env`
   - Check your API usage and limits

3. **Visualization Errors**
   - Ensure the data format matches expected structure
   - Check for missing or null values

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details. 