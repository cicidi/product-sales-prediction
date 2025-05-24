import pandas as pd
import os
from xgboost import XGBRegressor
from sklearn.pipeline import Pipeline
from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import OneHotEncoder, StandardScaler
from sklearn2pmml import sklearn2pmml, PMMLPipeline

def train_model_with_categorical():
  # === Load prepared data ===
  data = pd.read_csv("./data/prepared_daily_sales.csv", parse_dates=["create_timestamp"])

  # === Define feature columns ===
  categorical_features = ["product_id", "seller_id"]
  numeric_features = [
    "sale_price", "original_price", "is_holiday", "is_weekend",
    "day_of_week", "day_of_month", "month",
    "lag_1", "lag_7", "lag_30"
  ]
  all_features = categorical_features + numeric_features
  target = "quantity"

  X = data[all_features]
  y = data[target]

  # === Build preprocessing pipeline ===
  preprocessor = ColumnTransformer([
    ("cat", OneHotEncoder(handle_unknown="ignore"), categorical_features),
    ("num", "passthrough", numeric_features)
  ])

  pipeline = PMMLPipeline([
    ("preprocessor", preprocessor),
    ("regressor", XGBRegressor(n_estimators=100, max_depth=6, learning_rate=0.1, random_state=42))
  ])

  pipeline.fit(X, y)

  os.makedirs("model", exist_ok=True)
  sklearn2pmml(pipeline, "model/xgb_sales_predictor.pmml", with_repr=True)
  print("✅ PMML model saved to model/xgb_sales_predictor.pmml")

if __name__ == "__main__":
  train_model_with_categorical()
