import yaml
from typing import Dict, List, Any
import re

def load_openapi_spec(file_path: str) -> Dict[str, Any]:
    """Load and parse an OpenAPI specification from a YAML file."""
    with open(file_path, 'r') as file:
        return yaml.safe_load(file)

def extract_endpoints(openapi_spec: Dict[str, Any]) -> List[Dict[str, Any]]:
    """Extract API endpoints from an OpenAPI specification."""
    endpoints = []
    
    for path, path_item in openapi_spec.get('paths', {}).items():
        for method, operation in path_item.items():
            if method in ['get', 'post', 'put', 'delete', 'patch']:
                endpoint = {
                    'path': path,
                    'method': method.upper(),
                    'summary': operation.get('summary', ''),
                    'description': operation.get('description', ''),
                    'operationId': operation.get('operationId', ''),
                    'tags': operation.get('tags', []),
                    'parameters': operation.get('parameters', []),
                    'requestBody': operation.get('requestBody', {}),
                    'responses': operation.get('responses', {})
                }
                endpoints.append(endpoint)
    
    return endpoints

def format_endpoint_documentation(endpoint: Dict[str, Any]) -> str:
    """Format endpoint information for documentation purposes."""
    # Escape curly braces in the path to prevent ChatPromptTemplate from treating them as variables
    path = endpoint['path'].replace("{", "{{").replace("}", "}}")
    
    doc = f"{endpoint['method']} {path}\n"
    doc += f"Summary: {endpoint['summary']}\n"
    
    if endpoint['description']:
        # Escape any curly braces in the description
        description = endpoint['description'].replace("{", "{{").replace("}", "}}")
        doc += f"Description: {description}\n"
    
    if endpoint['tags']:
        doc += f"Tags: {', '.join(endpoint['tags'])}\n"
    
    # Parameters
    if endpoint['parameters']:
        doc += "Parameters:\n"
        for param in endpoint['parameters']:
            required = "required" if param.get('required', False) else "optional"
            # Escape any curly braces in parameter descriptions
            name = str(param.get('name', '')).replace("{", "{{").replace("}", "}}")
            param_in = str(param.get('in', '')).replace("{", "{{").replace("}", "}}")
            description = str(param.get('description', '')).replace("{", "{{").replace("}", "}}")
            doc += f"  - {name} ({param_in}, {required}): {description}\n"
    
    # Request Body
    if 'content' in endpoint.get('requestBody', {}):
        doc += "Request Body:\n"
        for content_type, content_schema in endpoint['requestBody'].get('content', {}).items():
            content_type = content_type.replace("{", "{{").replace("}", "}}")
            doc += f"  Content-Type: {content_type}\n"
            if 'schema' in content_schema:
                schema_ref = content_schema['schema'].get('$ref', '').split('/')[-1]
                if schema_ref:
                    schema_ref = schema_ref.replace("{", "{{").replace("}", "}}")
                    doc += f"  Schema: {schema_ref}\n"
    
    # Responses
    if endpoint['responses']:
        doc += "Responses:\n"
        for status_code, response in endpoint['responses'].items():
            description = str(response.get('description', '')).replace("{", "{{").replace("}", "}}")
            doc += f"  {status_code}: {description}\n"
    
    return doc

def get_all_endpoints_documentation(openapi_spec: Dict[str, Any]) -> str:
    """Get formatted documentation for all endpoints."""
    endpoints = extract_endpoints(openapi_spec)
    documentation = []
    
    for endpoint in endpoints:
        documentation.append(format_endpoint_documentation(endpoint))
    
    return "\n\n".join(documentation) 