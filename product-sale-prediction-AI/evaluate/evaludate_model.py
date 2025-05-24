import joblib
import pandas as pd
import numpy as np
from sklearn.metrics import mean_absolute_error, mean_squared_error, mean_absolute_percentage_error

def evaluate_sales_model(n_samples=100):
  # 加载模型
  model = joblib.load("../model/xgb_sales_predictor.pkl")
  print("✅ Model loaded successfully.")

  # 加载数据
  data = pd.read_csv("../data/prepared_daily_sales.csv", parse_dates=["create_timestamp"])
  data["create_date"] = data["create_timestamp"].dt.date

  # 筛掉含缺失值的数据（保险）
  data = data.dropna().reset_index(drop=True)

  # 采样
  eval_data = data.sample(n=n_samples, random_state=42)

  y_true = []
  y_pred = []
  error_percent = []

  for _, row in eval_data.iterrows():
    # 构造输入
    features = pd.DataFrame([{
      "product_id": row["product_id"],
      "seller_id": row["seller_id"],
      "sale_price": row["sale_price"],
      "original_price": row["original_price"],
      "is_holiday": row["is_holiday"],
      "is_weekend": row["is_weekend"],
      "day_of_week": row["day_of_week"],
      "day_of_month": row["day_of_month"],
      "month": row["month"],
      "lag_1": row["lag_1"],
      "lag_7": row["lag_7"],
      "lag_30": row["lag_30"]
    }])

    # 预测
    prediction = model.predict(features)[0]

    # 实际值：该 product + seller + date 的销量
    sample_date = row["create_date"]
    seller_id = row["seller_id"]
    product_id = row["product_id"]

    actual_quantity = data[
      (data["create_date"] == sample_date) &
      (data["seller_id"] == seller_id) &
      (data["product_id"] == product_id)
      ]["quantity"].sum()

    if actual_quantity > 0:
      y_true.append(actual_quantity)
      y_pred.append(prediction)
      error_percent.append(abs(prediction - actual_quantity) / actual_quantity * 100)

  # 计算误差指标
  mae = mean_absolute_error(y_true, y_pred)
  rmse = mean_squared_error(y_true, y_pred, squared=False)
  mape = mean_absolute_percentage_error(y_true, y_pred) * 100

  print(f"\n📊 Evaluation over {len(y_true)} valid samples:")
  print(f"MAE  (Mean Absolute Error):       {mae:.2f}")
  print(f"RMSE (Root Mean Squared Error):   {rmse:.2f}")
  print(f"MAPE (Mean Absolute % Error):     {mape:.2f}%")

  # 可视化（可选）
  try:
    import matplotlib.pyplot as plt

    plt.figure(figsize=(8, 4))
    plt.hist(error_percent, bins=20, edgecolor='black')
    plt.title("Error % Distribution")
    plt.xlabel("Absolute % Error")
    plt.ylabel("Frequency")
    plt.grid(True)
    plt.tight_layout()
    plt.show()
  except ImportError:
    print("📉 Skipping plot (matplotlib not installed)")

if __name__ == "__main__":
  evaluate_sales_model(n_samples=100)  # 你可以改成 n_samples=len(data) 跑全量
