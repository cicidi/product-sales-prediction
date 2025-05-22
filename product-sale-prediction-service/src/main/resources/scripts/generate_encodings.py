import pandas as pd
import numpy as np
import os

def generate_encoding_files():
    # 获取当前工作目录
    current_dir = os.getcwd()
    print(f"Current working directory: {current_dir}")
    
    # 创建模型目录（使用绝对路径）
    model_dir = os.path.abspath("../product-sale-prediction-AI/train/model")
    os.makedirs(model_dir, exist_ok=True)
    print(f"Created model directory: {model_dir}")
    
    # 读取CSV文件
    try:
        products_file = "src/main/resources/final_sample_products.csv"
        sales_file = "src/main/resources/sales_2023_2025_realistic.csv"
        
        print(f"Attempting to read products from: {os.path.abspath(products_file)}")
        print(f"Attempting to read sales from: {os.path.abspath(sales_file)}")
        
        products_df = pd.read_csv(products_file)
        sales_df = pd.read_csv(sales_file)
        
        print(f"Successfully loaded products data with {len(products_df)} rows")
        print(f"Successfully loaded sales data with {len(sales_df)} rows")
        
        # 获取唯一的产品ID和卖家ID
        unique_product_ids = products_df['product_id'].unique()
        unique_seller_ids = sales_df['seller_id'].unique()
        
        print(f"Found {len(unique_product_ids)} unique products")
        print(f"Found {len(unique_seller_ids)} unique sellers")
        
        # 创建编码映射
        product_encodings = {str(pid): idx for idx, pid in enumerate(unique_product_ids)}
        seller_encodings = {str(sid): idx for idx, sid in enumerate(unique_seller_ids)}
        
        # 保存产品ID编码
        product_encodings_file = os.path.join(model_dir, "product_id_encodings.csv")
        with open(product_encodings_file, "w") as f:
            for product_id, encoding in product_encodings.items():
                f.write(f"{product_id},{encoding}\n")
        print(f"Generated product encodings file: {product_encodings_file}")
        
        # 保存卖家ID编码
        seller_encodings_file = os.path.join(model_dir, "seller_id_encodings.csv")
        with open(seller_encodings_file, "w") as f:
            for seller_id, encoding in seller_encodings.items():
                f.write(f"{seller_id},{encoding}\n")
        print(f"Generated seller encodings file: {seller_encodings_file}")
        
        # 保存特征名称
        feature_names = [
            "unit_price", "dayofweek", "day", "week", "month", "quarter", "year",
            "is_weekend", "is_month_start", "is_month_end", "product_id_enc",
            "seller_id_enc", "lag_1_quantity", "lag_2_quantity", "lag_3_quantity",
            "lag_4_quantity", "lag_5_quantity", "lag_6_quantity", "lag_7_quantity",
            "lag_8_quantity", "lag_9_quantity", "lag_10_quantity", "lag_11_quantity",
            "lag_12_quantity", "lag_13_quantity", "lag_14_quantity",
            "rolling_mean_7d", "rolling_std_7d", "rolling_mean_14d", "rolling_std_14d",
            "rolling_mean_30d", "rolling_std_30d"
        ]
        
        feature_names_file = os.path.join(model_dir, "feature_names.txt")
        with open(feature_names_file, "w") as f:
            for feature in feature_names:
                f.write(f"{feature}\n")
        print(f"Generated feature names file: {feature_names_file}")
        
    except Exception as e:
        print(f"Error: {str(e)}")
        raise

if __name__ == "__main__":
    generate_encoding_files() 