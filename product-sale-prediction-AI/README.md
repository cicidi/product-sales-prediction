# 📊 Product Sales Prediction AI

A machine learning project for predicting product sales quantities using realistic synthetic data generation and advanced feature engineering.

## 🎯 Project Overview

This project simulates realistic e-commerce sales data and builds machine learning models to predict order quantities based on multiple factors including:
- Product characteristics (category, price, brand)
- Seller performance patterns
- Time-based factors (holidays, weekends, seasonality)
- Pricing and discount strategies

## 📁 Project Structure

```
product-sale-prediction-AI/
├── generate/                     # Data generation scripts
│   ├── generate_test_data.py     # Generate 100,000 records (full dataset)
│   ├── generate_small_test_data.py # Generate 1,000 records (quick testing)
│   ├── analyze_and_visualize_data.py # Data analysis and visualization
│   ├── DATA_GENERATION_README.md # Data generation documentation
│   └── sales_data_specification.md # Data schema specification
├── train/                        # Model training scripts
│   ├── prepare_sales_train_data.py # Prepare data for training
│   └── train_sales_model.py      # Train ML models
├── data/                         # Data files
│   ├── sales_2023_2025_realistic.csv # Generated sales data
│   ├── prepared_daily_sales.csv  # Processed training data
│   ├── final_sample_products.csv # Product metadata
│   ├── US_Federal_Holidays_2023_2030.csv # Holiday data
│   └── charts/                   # Generated analysis charts
├── model/                        # Trained models (generated)
└── requirements.txt              # Python dependencies
```

## 🚀 Quick Start Guide

### Step 1: Environment Setup

```bash
# Clone the repository
git clone <repository-url>
cd product-sale-prediction-AI

# Create virtual environment
python -m venv .venv
source .venv/bin/activate  # On Windows: .venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt
```

### Step 2: Generate Sales Data

Choose one of the following options:

**Option A: Quick Testing (1,000 records)**
```bash
python generate/generate_small_test_data.py
```

**Option B: Full Dataset (100,000 records)**
```bash
python generate/generate_test_data.py
```

This will create:
- `data/sales_2023_2025_realistic.csv` - Main sales data
- `data/products.csv` - Product metadata

### Step 3: Prepare Training Data

Process the raw sales data for machine learning:

```bash
python train/prepare_sales_train_data.py
```

This will:
- ✅ Check if all required files exist
- 🔄 Merge sales data with product and holiday information
- 📅 Add time-based features (weekday, month, holidays)
- 📈 Create lag features for time series analysis
- 💾 Save processed data to `data/prepared_daily_sales.csv`

### Step 4: Train Machine Learning Model

```bash
python train/train_sales_model.py
```

This will:
- 🤖 Train XGBoost regression model
- 📊 Display feature importance analysis
- 💾 Save trained model to `model/` directory
- 📈 Show model performance metrics

### Step 5: Data Analysis (Optional)

Generate comprehensive data analysis charts:

```bash
python generate/analyze_and_visualize_data.py
```

This creates multiple visualization files in `data/charts/`:
- Product-seller distribution heatmaps
- Feature impact analysis
- Time series trends
- Seasonal patterns
- Price-quantity correlations

## 📊 Data Schema

### Sales Data (`sales_2023_2025_realistic.csv`)
| Column | Type | Description |
|--------|------|-------------|
| `order_id` | String | Unique order identifier (UUID) |
| `product_id` | String | Product identifier (p101, p102, etc.) |
| `buyer_id` | String | Customer identifier |
| `seller_id` | String | Seller identifier (seller_1 to seller_6) |
| `unit_price` | Float | Sale price after discount |
| `quantity` | Integer | **TARGET**: Quantity ordered |
| `subtotal` | Float | Total amount (unit_price × quantity) |
| `create_timestamp` | DateTime | Order timestamp |

### Key Features for ML Model
- **Product factors**: Category, original price, discount rate
- **Seller factors**: Seller performance patterns
- **Time factors**: Holidays, weekends, seasonality, day of week/month
- **Price factors**: Unit price, discount percentage
- **Lag features**: Previous sales patterns (1, 7, 30 days)

## 🎯 Model Performance

The trained XGBoost model achieves:
- **R² Score**: ~0.85+ (explains 85%+ of quantity variance)
- **Feature Importance**: Balanced across multiple feature types
- **No Single Dominant Feature**: Prevents overfitting to one variable

## 🛠️ Troubleshooting

### Common Issues

**1. Module not found errors**
```bash
# Ensure virtual environment is activated
source .venv/bin/activate
pip install -r requirements.txt
```

**2. File not found errors**
```bash
# Generate data first
python generate/generate_small_test_data.py
# Then prepare training data
python train/prepare_sales_train_data.py
```

**3. Import errors in analysis**
```bash
# Install visualization dependencies
pip install matplotlib seaborn plotly
```

### File Dependency Chain

```
1. generate_test_data.py → sales_2023_2025_realistic.csv
2. prepare_sales_train_data.py → prepared_daily_sales.csv
3. train_sales_model.py → model files
4. analyze_and_visualize_data.py → charts (optional)
```

## 📋 Requirements

- Python 3.8+
- pandas >= 2.2.0
- numpy >= 1.21
- scikit-learn == 1.5.2
- xgboost >= 1.6
- matplotlib >= 3.5.0
- seaborn >= 0.11.0
- plotly >= 5.18.0

## 📄 License

This project is licensed under the MIT License.

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/improvement`)
3. Commit changes (`git commit -am 'Add new feature'`)
4. Push to branch (`git push origin feature/improvement`)
5. Create Pull Request

## 📚 Documentation

- [Data Generation Guide](generate/DATA_GENERATION_README.md)
- [Data Schema Specification](generate/sales_data_specification.md)
- [Feature Importance Analysis](FEATURE_IMPORTANCE_ANALYSIS.md)

---

**🎯 Ready to start? Run the commands in order and build your sales prediction model!**
