import json
import requests
from typing import Dict, Any, Optional, List
from urllib.parse import urljoin

class APIService:
    def __init__(self, base_url: str, api_key: Optional[str] = None):
        """Initialize the API service with a base URL and optional API key."""
        self.base_url = base_url
        self.headers = {}
        if api_key:
            self.headers["api-key"] = api_key
        
    def call_api(self, 
                method: str, 
                path: str, 
                params: Optional[Dict[str, Any]] = None, 
                data: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        """Make an API call with the given method, path, params, and data."""
        url = urljoin(self.base_url, path)
        
        # Make sure method is uppercase
        method = method.upper()
        
        # Initialize response as error in case request fails
        response = {"error": "Failed to make API call"}
        
        try:
            if method == "GET":
                resp = requests.get(url, params=params, headers=self.headers)
            elif method == "POST":
                resp = requests.post(url, params=params, json=data, headers=self.headers)
            elif method == "PUT":
                resp = requests.put(url, params=params, json=data, headers=self.headers)
            elif method == "DELETE":
                resp = requests.delete(url, params=params, headers=self.headers)
            elif method == "PATCH":
                resp = requests.patch(url, params=params, json=data, headers=self.headers)
            else:
                return {"error": f"Unsupported HTTP method: {method}"}
            
            # Check if the response is valid JSON
            try:
                response = resp.json()
            except json.JSONDecodeError:
                response = {"content": resp.text, "status_code": resp.status_code}
            
            # Add status code to response
            response["status_code"] = resp.status_code
            
        except requests.exceptions.RequestException as e:
            response = {"error": str(e)}
        
        return response
    
    def validate_params(self, 
                       required_params: List[str], 
                       provided_params: Dict[str, Any]) -> Optional[Dict[str, str]]:
        """Validate that all required parameters are provided."""
        missing_params = [param for param in required_params if param not in provided_params]
        if missing_params:
            return {"error": f"Missing required parameters: {', '.join(missing_params)}"}
        return None 