import pandas as pd


def prepare_data():
  # === file path ===
  orders_path = "train/data/sales_2023_2025_realistic.csv"
  products_path = "train/data/final_sample_products.csv"
  holidays_path = "train/data/US_Federal_Holidays_2023_2030.csv"

  # === load data ===
  orders = pd.read_csv(orders_path, parse_dates=["create_timestamp"])
  products = pd.read_csv(products_path, usecols=["id", "category", "price"])
  holidays = pd.read_csv(holidays_path, parse_dates=["date"])

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
  daily_sales.to_csv("train/data/prepared_daily_sales.csv", index=False)
  print("Data save to prepared_daily_sales.csv")
