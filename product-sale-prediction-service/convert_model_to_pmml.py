import joblib
import pandas as pd
import numpy as np
from sklearn2pmml import sklearn2pmml
from sklearn2pmml.pipeline import PMMLPipeline
from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import StandardScaler
import xgboost as xgb
import os

def convert_model_to_pmml():
    # Model file paths
    model_dir = "../product-sale-prediction-AI/train/model"
    model_path = os.path.join(model_dir, "xgb_sales_forecast_model.joblib")
    pmml_path = os.path.join(model_dir, "xgb_sales_forecast_model.pmml")
    
    try:
        # Load model
        print("Loading XGBoost model...")
        xgb_model = joblib.load(model_path)
        
        # Print model information
        print("\nModel Information:")
        print(f"Model type: {type(xgb_model)}")
        print(f"Model parameters: {xgb_model.get_params()}")
        print(f"Number of features used by model: {xgb_model.n_features_in_}")
        
        # Use original feature names of the model
        feature_names = [
            "unit_price", "dayofweek", "day", "week", "month", "quarter", "year",
            "is_weekend", "is_month_start", "is_month_end", "product_id_enc",
            "seller_id_enc", "lag_1_quantity", "lag_2_quantity", "lag_3_quantity",
            "lag_4_quantity", "lag_5_quantity", "lag_6_quantity", "lag_7_quantity",
            "lag_8_quantity", "lag_9_quantity", "lag_10_quantity", "lag_11_quantity",
            "lag_12_quantity", "lag_13_quantity", "lag_14_quantity",
            "rolling_mean_7d", "rolling_std_7d", "rolling_mean_14d", "rolling_std_14d",
            "rolling_mean_30d", "rolling_std_30d"
        ]
        
        print(f"\nUsing {len(feature_names)} features: {feature_names}")
        
        # Create example data
        print("\nCreating example data...")
        X = pd.DataFrame(np.random.rand(100, len(feature_names)), columns=feature_names)
        y = np.random.rand(100)  # Example target variable
        
        # Create preprocessor
        print("Creating preprocessor...")
        preprocessor = ColumnTransformer(
            transformers=[
                ('num', StandardScaler(), feature_names)
            ],
            remainder='passthrough'
        )
        
        # Create new XGBoost model using original model parameters
        print("Creating new XGBoost model...")
        new_xgb = xgb.XGBRegressor(
            n_estimators=xgb_model.n_estimators,
            max_depth=xgb_model.max_depth,
            learning_rate=xgb_model.learning_rate,
            objective='reg:squarederror',
            colsample_bytree=0.8,
            subsample=0.8,
            min_child_weight=3,
            random_state=42
        )
        
        # Create and train pipeline
        print("Creating and training pipeline...")
        pipeline = PMMLPipeline([
            ("preprocessor", preprocessor),
            ("regressor", new_xgb)
        ])
        
        # Train pipeline
        print("Fitting pipeline...")
        pipeline.fit(X, y)
        
        # Copy original model parameters to new model
        print("Copying model parameters...")
        new_xgb._Booster = xgb_model._Booster
        
        # Convert to PMML
        print("Converting to PMML...")
        sklearn2pmml(pipeline, pmml_path, with_repr=True)
        
        print(f"Successfully converted model to PMML format: {pmml_path}")
        return True
        
    except Exception as e:
        print(f"Error converting model to PMML: {str(e)}")
        return False

if __name__ == "__main__":
    convert_model_to_pmml() 