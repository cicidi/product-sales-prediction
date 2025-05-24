# 📊 Sales Data Generation & Analysis Guide

This directory contains comprehensive data generation and analysis scripts for creating realistic e-commerce sales data that meets machine learning requirements. All data specifications are detailed in [`sales_data_specification.md`](sales_data_specification.md).

## 📁 Script Overview

### Core Data Generation
| Script | Purpose | Records | Runtime | Use Case |
|--------|---------|---------|---------|-----------|
| `generate_test_data.py` | Complete dataset | 100,000 | 2-3 min | Full ML training |
| `generate_small_test_data.py` | Quick testing | 1,000 | 5-10 sec | Development & testing |

### Analysis & Visualization
| Script | Purpose | Output | Dependencies |
|--------|---------|---------|-------------|
| `analyze_and_visualize_data.py` | Comprehensive analysis | Charts & reports | matplotlib, seaborn, plotly |

## 🚀 Quick Start Workflow

### Step 1: Generate Data
```bash
# For quick testing (recommended first)
python generate_small_test_data.py

# For complete ML training dataset
python generate_test_data.py
```

### Step 2: Analyze Generated Data
```bash
# Generate comprehensive analysis charts and reports
python analyze_and_visualize_data.py
```

## 📊 Output Structure

### Generated Data Files
```
data/
├── sales_2023_2025_realistic.csv    # Main order data
├── products.csv                     # Product metadata
└── charts/                          # Analysis visualizations
    ├── 00_data_validation_report.md
    ├── 01_product_seller_distribution_heatmap.png
    ├── 02_product_seller_count_distribution.html
    ├── 03_feature_impact_on_quantity_analysis.png
    ├── 04_time_series_analysis.html
    ├── 05_product_performance_comparison.html
    ├── 06_seasonal_analysis.png
    └── 07_price_quantity_correlation.html
```

### Data Schemas
For detailed field descriptions and data types, see [`sales_data_specification.md`](sales_data_specification.md).

**Sales Data Schema:**
```csv
order_id,product_id,buyer_id,seller_id,unit_price,quantity,subtotal,create_timestamp
```

**Product Data Schema:**
```csv
id,name,category,brand,price,createTimeStamp,description
```

## 🎯 Feature Engineering & Importance

### Realistic E-commerce Patterns
The data generation implements **balanced feature importance** to avoid unrealistic model dominance:

| Feature Type | Target Importance | Implementation |
|--------------|------------------|----------------|
| **Product Characteristics** | 25-35% | Category-based quantity patterns |
| **Pricing & Discounts** | 20-30% | Moderate price sensitivity (150% max) |
| **Seller Reputation** | 15-25% | Realistic seller advantage (15%) |
| **Temporal Factors** | 10-20% | Seasonal, holiday, monthly effects |
| **Weekend/Day Effects** | 5-15% | Category-specific weekend patterns |

### Key Adjustments for Realism
```python
# Weekend effects (category-specific)
Food: +8% weekend increase
Electronics: +5% weekend increase  
Clothing: +3% weekend increase

# Holiday effects
All categories: +15% holiday boost (reduced from 40%)

# Price sensitivity
Discount impact: 150% maximum (reduced from 200%)

# Seller influence  
Primary seller advantage: +15% (reduced from 30%)
```

## 📈 Comprehensive Data Analysis

### Analysis Features
The `analyze_and_visualize_data.py` script generates:

#### 1. **Distribution Analysis**
- Product-seller relationship heatmaps
- Seller coverage verification
- Order count distributions

#### 2. **Feature Impact Analysis**
- Discount rate vs quantity correlation
- Product category influence patterns
- Seller performance variations
- Temporal factor impacts (weekday, month, weekend)

#### 3. **Time Series Analysis**
- Daily sales quantity trends
- Revenue pattern analysis
- Order count fluctuations
- Interactive time series charts

#### 4. **Product Performance Analysis**
- Cross-product quantity comparisons
- Average unit price analysis
- Discount rate distributions
- Revenue performance metrics

#### 5. **Seasonal Pattern Analysis**
- Monthly quantity trends by category
- Seasonal clothing patterns
- Year-end electronics surges
- Food consumption consistency

#### 6. **Price-Quantity Correlation**
- Interactive scatter plots with trend lines
- Category-specific price sensitivity
- Discount effectiveness analysis

### Generated Reports
- **Data Validation Report**: Comprehensive markdown summary
- **Static Charts**: PNG files for presentations
- **Interactive Visualizations**: HTML files for detailed exploration

## 🎛️ Business Logic Implementation

### Product Categories & Base Patterns
```python
Electronics: Base quantity 1.5, Max 5 items
Clothing:    Base quantity 2.0, Max 8 items  
Food:        Base quantity 3.0, Max 15 items
```

### Temporal Effects
- **Holidays**: 15% boost for electronics/clothing
- **Weekend**: Category-specific increases (3-8%)
- **Month-start**: 5% payday effect
- **Seasonal**: 10-15% category-specific variations

### Seller Distribution
- **seller_1**: Major seller (all products)
- **seller_2-6**: Specialized sellers (2-3 products each)
- Guaranteed: No seller without products

### Pricing Strategy
- **Base discount**: 15% average
- **Holiday periods**: Additional 10% off
- **Weekend food**: Additional 5% off
- **Month-end clearance**: Additional 5% off

## 🔍 Data Quality Validation

### Automatic Checks
- ✅ All sellers have products assigned
- ✅ Balanced quantity distributions
- ✅ Realistic discount ranges (0-30%)
- ✅ Proper temporal distributions
- ✅ No missing critical data

### Feature Importance Validation
Target thresholds (post-training):
- ❌ No single feature >30% importance
- ✅ Weekend effect <15%
- ✅ Price factors 20-30%
- ✅ Balanced product representation

## 🛠️ Dependencies & Installation

### Required Packages
```bash
# Core data generation
pip install pandas numpy uuid

# Analysis & visualization
pip install matplotlib seaborn plotly

# Or install all at once
pip install -r ../requirements.txt
```

### System Requirements
- Python 3.8+
- 4GB+ RAM for full dataset generation
- 100MB+ disk space for outputs

## 🐛 Troubleshooting Guide

### Common Issues

**1. Data Generation Failures**
```bash
# Ensure data directory exists
mkdir -p ../data

# Check write permissions
ls -la ../data/
```

**2. Chart Generation Errors**
```bash
# Install visualization dependencies
pip install matplotlib seaborn plotly

# For font issues (Linux)
sudo apt-get install fonts-dejavu-core
```

**3. File Path Issues**
```bash
# Run from project root directory
cd /path/to/product-sale-prediction-AI
python generate/generate_test_data.py
```

## 📚 Related Documentation

- **[Data Schema Specification](sales_data_specification.md)** - Detailed field descriptions
- **[Feature Importance Analysis](../FEATURE_IMPORTANCE_ANALYSIS.md)** - ML model optimization guide
- **[Main Project README](../README.md)** - Overall project workflow

## 🔄 Recommended Workflow

1. **Start Small**: Run `generate_small_test_data.py` first
2. **Validate Output**: Check generated data structure
3. **Full Generation**: Run `generate_test_data.py` for ML training
4. **Analyze Results**: Execute `analyze_and_visualize_data.py`
5. **Review Reports**: Check generated charts and validation report
6. **Proceed to Training**: Use data for ML model development

---

**💡 Tip**: Always review the generated charts before proceeding to model training to ensure data quality meets your specific requirements! 