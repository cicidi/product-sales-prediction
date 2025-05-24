# 🧪 Synthetic Sales Order Dataset Specification

This document describes the structure, logic, and constraints for generating a **100,000-row synthetic sales dataset**. It is designed to support realistic **regression analysis**, especially for predicting `quantity` based on multiple features with balanced influence.

---

## 🎯 Objective

Simulate historical order data for 11 products and 6 sellers from `2023-01-01` to `2025-05-24`, with realistic purchasing patterns influenced by features such as pricing, calendar effects, and product type. The goal is to ensure **no single feature dominates** the predictive model (e.g. `product_id_p102`), and all features contribute meaningful signal to a machine learning model.

---

## 📦 Data Schema

### **Order Data Format**

```csv
order_id,product_id,buyer_id,seller_id,unit_price,quantity,subtotal,create_timestamp
```

| Field            | Type        | Description                                 |
|------------------|-------------|---------------------------------------------|
| `order_id`       | UUID        | Unique order identifier                     |
| `product_id`     | Categorical | Product ID                                  |
| `buyer_id`       | Categorical | Buyer ID (simulated users)                  |
| `seller_id`      | Categorical | One of seller_1 to seller_6                 |
| `unit_price`     | Numeric     | Sale price after discount                   |
| `quantity`       | Numeric     | Quantity sold (regression target)           |
| `subtotal`       | Numeric     | `unit_price × quantity`                     |
| `create_timestamp` | Timestamp | Order creation time                         |

---

### **Product Data Format**

```csv
id,name,category,brand,price,createTimeStamp,description
```

Includes 11 unique products across 3 categories:
- **Electronics**: iPhones, Galaxy, Pixel
- **Clothes**: Nike shoes, Uniqlo jacket, Adidas T-shirt
- **Food**: Almond butter, chocolate, roasted almonds

Each product has a fixed `original_price`.

---

## 🧑‍💼 Seller Setup

| Seller        | Type    | Inventory Coverage               |
|---------------|---------|----------------------------------|
| `seller_1`    | Large   | Sells all or most products       |
| `seller_2-6`  | Smart   | Sell a few of the products       |
| Each product  | Sold by | 2 to 3 sellers only (randomized) |

---

## 🕒 Time Features

All orders span **2023-01-01 to 2025-05-24**.

Derived features from `create_timestamp`:
- `day_of_week`: 0 (Mon) – 6 (Sun)
- `day_of_month`: 1 – 31
- `month`: 1 – 12
- `is_weekend`: True if Saturday or Sunday
- `is_holiday`: Simulated US holidays (New Year, Independence Day, Christmas)

---

## 📈 Quantity Simulation Logic

`quantity` is a function of several features:

| Feature             | Effect on Quantity                               |
|---------------------|--------------------------------------------------|
| `sale_price` vs `original_price` | Greater discount → higher quantity (up to 20% discount) |
| `is_holiday`        | Boosts electronics and clothes sales             |
| `is_weekend`        | Boosts food sales significantly                  |
| `product_id`        | Different base rates, all balanced               |
| `seller_id`         | Large sellers have slightly higher volume        |
| `day_of_week`       | Captures weekday/weekend patterns                |
| `day_of_month` / `month` | Reflect seasonal/monthly variations        |

Quantity values are drawn from a **Poisson distribution**, adjusted dynamically.

---

## ⚠️ Modeling Constraints

| Issue                                | Solution                                        |
|-------------------------------------|-------------------------------------------------|
| One feature dominates (e.g. p102)   | Control max quantity per product, rebalance     |
| Original price has no impact        | Ensure correlation with discount ratio          |
| Low importance of time features     | Inject variance via calendar-based modifiers    |
| Sparse product/category importance  | Force distribution balance in simulation        |

---

## ✅ Feature Importance Design Goal

| Feature Type       | Examples                         | Expected Contribution |
|--------------------|----------------------------------|------------------------|
| Categorical         | `product_id`, `seller_id`         | High but shared        |
| Numerical (Price)   | `sale_price`, `original_price`    | Medium-high            |
| Calendar            | `is_holiday`, `month`, `weekend` | Medium                 |
| Time Series (Optional) | `lag_1`, `lag_7`, `lag_30`     | Medium                 |

No single feature (especially `product_id`) should account for >50% of model importance.

---

## 📁 Output Files

- `orders.csv`: all simulated orders (100,000 rows)
- `products.csv`: static product metadata
- [Optional] `daily_sales.csv`: aggregated by product/day for time series modeling

---

## 🛠️ Next Steps

- Train regression model (e.g. XGBoost)
- Use feature importance analysis to validate distribution
- Add lag features, product age, or demand windows as next iteration

