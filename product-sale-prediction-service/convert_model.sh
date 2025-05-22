#!/bin/bash

# Check Python environment
if ! command -v python3 &> /dev/null; then
    echo "Python3 is required but not found. Please install Python3 first."
    exit 1
fi

# Create virtual environment
echo "Creating Python virtual environment..."
python3 -m venv venv

# Activate virtual environment
echo "Activating virtual environment..."
source venv/bin/activate

# Install required dependencies
echo "Installing required packages..."
pip install --upgrade pip
pip install joblib pandas numpy 'scikit-learn==1.5.2' xgboost sklearn2pmml sklearn-pandas

# Generate encoding files
echo "Generating encoding files..."
python generate_encodings.py

# Run conversion script
echo "Running model conversion script..."
python convert_model_to_pmml.py

# Check conversion result
if [ $? -eq 0 ]; then
    echo "Model conversion completed successfully."
else
    echo "Model conversion failed."
    exit 1
fi

# Deactivate virtual environment
deactivate

echo "Done." 