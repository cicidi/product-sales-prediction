import joblib
import pandas as pd

def test_sales_model_with_correct_actual():
  # 加载模型
  model = joblib.load("model/xgb_sales_predictor.pkl")
  print("✅ Model loaded successfully.")

  # 加载聚合后的数据
  data = pd.read_csv("data/prepared_daily_sales.csv", parse_dates=["create_timestamp"])
  data["create_date"] = data["create_timestamp"].dt.date

  # 随机选取一个样本
  sample = data.sample(n=1000, random_state=42).iloc[0]

  # 构造模型输入
  test_sample = pd.DataFrame([{
    "product_id": sample["product_id"],
    "seller_id": sample["seller_id"],
    "sale_price": sample["sale_price"],
    "original_price": sample["original_price"],
    "is_holiday": sample["is_holiday"],
    "is_weekend": sample["is_weekend"],
    "day_of_week": sample["day_of_week"],
    "day_of_month": sample["day_of_month"],
    "month": sample["month"],
    "lag_1": sample["lag_1"],
    "lag_7": sample["lag_7"],
    "lag_30": sample["lag_30"]
  }])

  # 模型预测
  predicted_quantity = model.predict(test_sample)[0]

  # 真实值：该 seller + product 在该日的实际销量
  sample_date = sample["create_date"]
  sample_seller = sample["seller_id"]
  sample_product = sample["product_id"]

  actual_quantity = data[
    (data["create_date"] == sample_date) &
    (data["seller_id"] == sample_seller) &
    (data["product_id"] == sample_product)
    ]["quantity"].sum()

  # 输出结果
  print("📆 Sample date:", sample_date)
  print("🛒 Seller/Product:", sample_seller, sample_product)
  print(f"📈 Predicted quantity: {predicted_quantity:.2f}")
  print(f"📉 Actual quantity: {actual_quantity:.2f}")

  if actual_quantity > 0:
    percent_error = abs(predicted_quantity - actual_quantity) / actual_quantity * 100
    print(f"📉 % Error vs actual: {percent_error:.2f}%")

if __name__ == "__main__":
  test_sales_model_with_correct_actual()
