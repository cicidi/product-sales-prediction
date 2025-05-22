#!/usr/bin/env python3
"""
Convert XGBoost model from joblib format to PMML format
This allows Java applications to directly load and use the model
"""

import os
import sys
import joblib
import pandas as pd
import numpy as np
from sklearn2pmml import sklearn2pmml
from sklearn2pmml.pipeline import PMMLPipeline
from sklearn.preprocessing import LabelEncoder
import argparse

def convert_model_to_pmml(model_dir, output_dir=None):
    """
    Convert XGBoost model from joblib format to PMML format
    
    Args:
        model_dir: Model file directory
        output_dir: Output directory, defaults to model_dir
    """
    if output_dir is None:
        output_dir = model_dir
    
    # Check if directory exists
    if not os.path.exists(model_dir):
        raise ValueError(f"Model directory does not exist: {model_dir}")
    
    os.makedirs(output_dir, exist_ok=True)
    
    # Load model and encoders
    model_path = os.path.join(model_dir, "xgb_sales_forecast_model.joblib")
    product_encoder_path = os.path.join(model_dir, "label_encoder_product_id.joblib")
    seller_encoder_path = os.path.join(model_dir, "label_encoder_seller_id.joblib")
    feature_names_path = os.path.join(model_dir, "feature_names.joblib")
    
    print(f"Loading model: {model_path}")
    model = joblib.load(model_path)
    
    print(f"Loading product encoder: {product_encoder_path}")
    product_encoder = joblib.load(product_encoder_path)
    
    print(f"Loading seller encoder: {seller_encoder_path}")
    seller_encoder = joblib.load(seller_encoder_path)
    
    print(f"Loading feature names: {feature_names_path}")
    feature_names = joblib.load(feature_names_path)
    
    # Create PMML pipeline
    pipeline = PMMLPipeline([
        ("model", model)
    ])
    
    # Set feature names
    pipeline.configure(features=feature_names)
    
    # Export PMML file
    pmml_path = os.path.join(output_dir, "xgb_sales_forecast_model.pmml")
    print(f"Exporting PMML file: {pmml_path}")
    sklearn2pmml(pipeline, pmml_path)
    
    # Export encoding mappings to CSV files for Java to load
    export_encodings_to_csv(product_encoder, "product_id", output_dir)
    export_encodings_to_csv(seller_encoder, "seller_id", output_dir)
    
    # Export feature names to text file
    feature_names_txt_path = os.path.join(output_dir, "feature_names.txt")
    with open(feature_names_txt_path, 'w') as f:
        for feature in feature_names:
            f.write(f"{feature}\n")
    
    print("Conversion complete!")
    print(f"Model exported to: {pmml_path}")
    print(f"Product ID encodings exported to: {os.path.join(output_dir, 'product_id_encodings.csv')}")
    print(f"Seller ID encodings exported to: {os.path.join(output_dir, 'seller_id_encodings.csv')}")
    print(f"Feature names exported to: {feature_names_txt_path}")

def export_encodings_to_csv(encoder, name, output_dir):
    """
    Export LabelEncoder mappings to CSV file
    
    Args:
        encoder: LabelEncoder object
        name: Encoder name (product_id or seller_id)
        output_dir: Output directory
    """
    encodings = {}
    for i, class_value in enumerate(encoder.classes_):
        encodings[class_value] = i
    
    # Export to CSV
    output_path = os.path.join(output_dir, f"{name}_encodings.csv")
    with open(output_path, 'w') as f:
        for key, value in encodings.items():
            f.write(f"{key},{value}\n")
    
    print(f"Exported {len(encodings)} {name} encodings to {output_path}")

def main():
    parser = argparse.ArgumentParser(description='Convert XGBoost model from joblib format to PMML format')
    parser.add_argument('--model-dir', type=str, default='../product-sale-prediction-AI/train/model',
                        help='Model file directory')
    parser.add_argument('--output-dir', type=str, default=None,
                        help='Output directory, defaults to model_dir')
    
    args = parser.parse_args()
    try:
        convert_model_to_pmml(args.model_dir, args.output_dir)
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main() 