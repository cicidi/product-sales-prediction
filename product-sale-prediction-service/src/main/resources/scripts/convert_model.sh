#!/bin/bash

# This script runs the model conversion script to convert XGBoost model from joblib format to PMML format

# Get absolute path of current script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

# Model directory and output directory
MODEL_DIR="../../../product-sale-prediction-AI/train/model"
OUTPUT_DIR="$MODEL_DIR"

# Check Python environment
if ! command -v python3 &> /dev/null; then
  echo "Error: Python 3 is required but not found"
  exit 1
fi

# Install required Python packages
echo "Checking required Python packages..."
python3 -m pip install -q joblib pandas numpy scikit-learn sklearn2pmml

# Run conversion script
echo "Running model conversion script..."
cd "$SCRIPT_DIR" && python3 convert_model_to_pmml.py --model-dir "$MODEL_DIR" --output-dir "$OUTPUT_DIR"

# Check script execution result
if [ $? -eq 0 ]; then
  echo "Model successfully converted to PMML format"
  echo "Now the model can be directly loaded and used by Java MLModelService"
else
  echo "Model conversion failed"
  exit 1
fi 