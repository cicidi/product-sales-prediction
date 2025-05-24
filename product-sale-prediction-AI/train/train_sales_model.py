import pandas as pd
import numpy as np
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler
from sklearn2pmml import sklearn2pmml, PMMLPipeline
from xgboost import XGBRegressor

from train.prepare_sales_train_data import prepare_data


def train_model():
  # === Load prepared data ===
  data = pd.read_csv("./data/prepared_daily_sales.csv",
                     parse_dates=["create_timestamp"])

  # === Define input features and target column ===
  features = [
    "sale_price", "original_price", "is_holiday", "is_weekend",
    "day_of_week", "day_of_month", "month",
    "lag_1", "lag_7", "lag_30"
  ]
  target = "quantity"

  X = data[features]
  y = data[target]

  # === Define PMML-compatible pipeline ===
  # Step 1: Scale features using StandardScaler
  # Step 2: Train an XGBoost regressor
  pipeline = PMMLPipeline([
    ("scaler", StandardScaler()),
    ("regressor",
     XGBRegressor(n_estimators=100, max_depth=6, learning_rate=0.1))
  ])

  # === Train model ===
  pipeline.fit(X, y)

  # === Export to PMML format ===
  # This PMML file can be used in Java with JPMML
  sklearn2pmml(pipeline, "./model/xgb_sales_predictor.pmml", with_repr=True)
  print("PMML model saved to model/xgb_sales_predictor.pmml")

def main():
  prepare_data()
  train_model()


if __name__ == "__main__":
  main()


