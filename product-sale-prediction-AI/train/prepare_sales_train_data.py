import pandas as pd
import os


def prepare_data():
  # === file path ===
  orders_path = "../data/sales_2023_2025_realistic.csv"
  products_path = "../data/final_sample_products.csv"
  holidays_path = "../data/US_Federal_Holidays_2023_2030.csv"

  # === check if files exist ===
  print("Checking required data files...")
  
  files_to_check = [
    (orders_path, "Sales data file", "Run: python generate/generate_test_data.py or python generate/generate_small_test_data.py"),
    (products_path, "Products data file", "This file should already exist"),
    (holidays_path, "Holidays data file", "This file should already exist")
  ]
  
  missing_files = []
  for file_path, description, suggestion in files_to_check:
    if os.path.exists(file_path):
      print(f"✅ {description}: {file_path}")
    else:
      print(f"❌ {description}: {file_path} - NOT FOUND")
      print(f"   Suggestion: {suggestion}")
      missing_files.append(file_path)
  
  if missing_files:
    print(f"\n❌ Error: {len(missing_files)} required file(s) missing!")
    print("Please generate missing files before running this script.")
    return None
  
  print("\n✅ All required files found! Proceeding with data preparation...\n")

  # === load data ===
  try:
    orders = pd.read_csv(orders_path, parse_dates=["create_timestamp"])
    products = pd.read_csv(products_path, usecols=["id", "category", "price"])
    holidays = pd.read_csv(holidays_path, parse_dates=["date"])
    print(f"✅ Data loaded successfully!")
    print(f"   - Orders: {len(orders):,} records")
    print(f"   - Products: {len(products)} products")
    print(f"   - Holidays: {len(holidays)} holidays")
  except Exception as e:
    print(f"❌ Error loading data: {str(e)}")
    return None

  # === mapping data ===
  orders.rename(columns={
    "unit_price": "sale_price"
  }, inplace=True)

  products.rename(columns={
    "id": "product_id",
    "price": "original_price"
  }, inplace=True)

  df = pd.merge(orders, products[["product_id", "category", "original_price"]],
                on="product_id", how="left")

  # === time feature ===
  df["date"] = df["create_timestamp"].dt.date
  df["date"] = pd.to_datetime(df["date"])
  df["is_weekend"] = (df["create_timestamp"].dt.weekday >= 5).astype(int)
  df["day_of_week"] = df["create_timestamp"].dt.weekday
  df["day_of_month"] = df["create_timestamp"].dt.day
  df["month"] = df["create_timestamp"].dt.month

  # === mapping holiday feature ===
  holidays["is_holiday"] = 1
  df = df.merge(holidays[["date", "is_holiday"]], on="date", how="left")
  df["is_holiday"] = df["is_holiday"].fillna(0).astype(int)

  # === aggregate daily sales ===
  daily_sales = df.groupby(["product_id", "seller_id", "create_timestamp"]).agg(
      {
        "sale_price": "mean",
        "original_price": "mean",
        "quantity": "sum",
        "is_holiday": "max",
        "is_weekend": "max",
        "day_of_week": "first",
        "day_of_month": "first",
        "month": "first"
      }).reset_index()

  # === lag features ===
  daily_sales = daily_sales.sort_values(
      ["product_id", "seller_id", "create_timestamp"])
  for lag in [1, 7, 30]:
    daily_sales[f"lag_{lag}"] = \
      daily_sales.groupby(["product_id", "seller_id"])["quantity"].shift(lag)

  # === default sale ===
  daily_sales = daily_sales.fillna(0)

  # === save data ===
  output_path = "../data/prepared_daily_sales.csv"
  daily_sales.to_csv(output_path, index=False)
  print(f"\n✅ Data preparation completed successfully!")
  print(f"📁 Prepared training data saved to: {output_path}")
  print(f"📊 Final dataset: {len(daily_sales):,} records")
  print(f"🎯 Ready for model training!")
  
  return daily_sales


if __name__ == "__main__":
  result = prepare_data()
  if result is not None:
    print("\n" + "="*50)
    print("Data preparation completed successfully!")
  else:
    print("\n" + "="*50)
    print("Data preparation failed!")
    print("Please check the error messages above.")
