#!/bin/bash

# This script runs the Python model server locally for sales predictions

MODEL_DIR="../../product-sale-prediction-AI/train"
FLASK_SCRIPT="$MODEL_DIR/deploy_locally.py"

# Check if the model directory exists
if [ ! -d "$MODEL_DIR" ]; then
  echo "Error: Model directory not found: $MODEL_DIR"
  exit 1
fi

# Check if the Flask script exists
if [ ! -f "$FLASK_SCRIPT" ]; then
  echo "Error: Flask script not found: $FLASK_SCRIPT"
  exit 1
fi

# Check if Python 3 is installed
if ! command -v python3 &> /dev/null; then
  echo "Error: Python 3 is required but not found"
  exit 1
fi

# Install required Python packages if not already installed
echo "Checking required packages..."
python3 -m pip install -q flask joblib pandas numpy xgboost scikit-learn

# Run the Flask server
echo "Starting model server..."
cd "$MODEL_DIR" && python3 deploy_locally.py --port 8000 