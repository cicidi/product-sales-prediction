import pandas as pd
import os
import joblib
from xgboost import XGBRegressor
from sklearn.pipeline import Pipeline
from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import StandardScaler, OneHotEncoder
from sklearn2pmml import PMMLPipeline, sklearn2pmml


def train_daily_sales_model():
  # Load original data
  data = pd.read_csv("./data/prepared_daily_sales.csv", parse_dates=["create_timestamp"])

  # Round to daily granularity
  data["create_timestamp"] = data["create_timestamp"].dt.date  # 保留日期部分

  # Add time features
  data["day_of_week"] = pd.to_datetime(data["create_timestamp"]).dt.dayofweek
  data["day_of_month"] = pd.to_datetime(data["create_timestamp"]).dt.day
  data["month"] = pd.to_datetime(data["create_timestamp"]).dt.month

  # Group by date + seller + product
  daily = data.groupby(["create_timestamp", "seller_id", "product_id"]).agg({
    "quantity": "sum",
    "sale_price": "mean",
    "original_price": "mean",
    "is_holiday": "first",
    "is_weekend": "first",
    "day_of_week": "first",
    "day_of_month": "first",
    "month": "first"
  }).reset_index()

  # Add lag features
  daily = daily.sort_values(by=["product_id", "seller_id", "create_timestamp"])
  for lag in [1, 7, 30]:
    daily[f"lag_{lag}"] = (
      daily.groupby(["seller_id", "product_id"])["quantity"]
      .shift(lag)
    )

  # Drop rows with NaN lag features
  daily = daily.dropna().reset_index(drop=True)

  # Features and target
  categorical_features = ["product_id", "seller_id"]
  numeric_features = [
    "sale_price", "original_price", "is_holiday", "is_weekend",
    "day_of_week", "day_of_month", "month", "lag_1", "lag_7", "lag_30"
  ]
  all_features = categorical_features + numeric_features
  X = daily[all_features]
  y = daily["quantity"]

  # Preprocessing: encode categorical and scale numeric
  preprocessor = ColumnTransformer([
    ("cat", OneHotEncoder(handle_unknown="ignore"), categorical_features),
    ("num", StandardScaler(), numeric_features)
  ])

  pipeline = PMMLPipeline([
    ("preprocessor", preprocessor),
    ("regressor", XGBRegressor(
        n_estimators=100,
        max_depth=6,
        learning_rate=0.1,
        random_state=42,
        objective="reg:squarederror"
    ))
  ])

  # Train model
  pipeline.fit(X, y)

  # Save model
  os.makedirs("model", exist_ok=True)
  try:
    sklearn2pmml(pipeline, "model/xgb_sales_predictor.pmml", with_repr=True)
    print("✅ PMML model saved to model/xgb_sales_predictor.pmml")
  except Exception as e:
    print(f"⚠️ PMML export failed: {str(e)}")

  joblib.dump(pipeline, "model/xgb_sales_predictor.pkl")
  print("✅ Pickle model saved to model/xgb_sales_predictor.pkl")


if __name__ == "__main__":
  train_daily_sales_model()
