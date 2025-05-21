#!/usr/bin/env python3
"""
本地模型预测脚本 - 无需SageMaker部署
"""
import os
import joblib
import numpy as np
import pandas as pd
import json
import argparse
import datetime
from flask import Flask, request, jsonify

# 模型和处理器文件路径
MODEL_DIR = "model"
MODEL_PATH = os.path.join(MODEL_DIR, "xgb_sales_forecast_model.joblib")
PRODUCT_ENCODER_PATH = os.path.join(MODEL_DIR, "label_encoder_product_id.joblib")
SELLER_ENCODER_PATH = os.path.join(MODEL_DIR, "label_encoder_seller_id.joblib")
FEATURE_NAMES_PATH = os.path.join(MODEL_DIR, "feature_names.joblib")
HISTORICAL_DATA_PATH = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 
                                  "resources", 
                                  "sales_2023_2025_realistic.csv")

# 全局变量
model = None
product_encoder = None
seller_encoder = None
feature_names = None
historical_data = None

def load_model_and_encoders():
    """加载模型和编码器"""
    global model, product_encoder, seller_encoder, feature_names
    
    print(f"加载模型: {MODEL_PATH}")
    model = joblib.load(MODEL_PATH)
    
    print(f"加载产品编码器: {PRODUCT_ENCODER_PATH}")
    product_encoder = joblib.load(PRODUCT_ENCODER_PATH)
    
    print(f"加载卖家编码器: {SELLER_ENCODER_PATH}")
    seller_encoder = joblib.load(SELLER_ENCODER_PATH)
    
    print(f"加载特征名称: {FEATURE_NAMES_PATH}")
    feature_names = joblib.load(FEATURE_NAMES_PATH)
    
    print("模型和编码器加载完成")
    
    # 打印一些信息
    print(f"可用产品数量: {len(product_encoder.classes_)}")
    print(f"可用卖家数量: {len(seller_encoder.classes_)}")
    print(f"部分产品ID示例: {product_encoder.classes_[:5]}")
    print(f"部分卖家ID示例: {seller_encoder.classes_[:5]}")
    
    return model, product_encoder, seller_encoder, feature_names

def load_historical_data():
    """加载历史销售数据"""
    global historical_data
    
    if historical_data is None:
        print(f"加载历史数据: {HISTORICAL_DATA_PATH}")
        if not os.path.exists(HISTORICAL_DATA_PATH):
            print(f"错误: 历史数据文件不存在: {HISTORICAL_DATA_PATH}")
            return pd.DataFrame()
            
        historical_data = pd.read_csv(HISTORICAL_DATA_PATH, parse_dates=["create_timestamp"])
        historical_data["create_timestamp"] = pd.to_datetime(historical_data["create_timestamp"])
        
        print(f"原始数据行数: {len(historical_data)}")
        print(f"数据列: {historical_data.columns.tolist()}")
        print(f"产品ID唯一值: {historical_data['product_id'].nunique()}")
        print(f"卖家ID唯一值: {historical_data['seller_id'].nunique()}")
        
        # 按product_id, seller_id和日期聚合
        historical_data = historical_data.groupby(
            ["product_id", "seller_id", pd.Grouper(key="create_timestamp", freq="D")]
        ).agg({
            "quantity": "sum",
            "unit_price": "mean"
        }).reset_index()
        
        # 按日期排序
        historical_data = historical_data.sort_values(["product_id", "seller_id", "create_timestamp"])
        print(f"聚合后数据行数: {len(historical_data)}")
    
    return historical_data

def get_historical_sales(product_id, seller_id, use_all_data=True):
    """获取特定产品和卖家的历史销售数据"""
    df = load_historical_data()
    
    if len(df) == 0:
        print(f"错误: 历史数据为空")
        return None
    
    # 筛选特定产品和卖家的数据
    mask = (df["product_id"] == product_id) & (df["seller_id"] == seller_id)
    product_data = df[mask].copy()
    
    print(f"产品 {product_id} 和卖家 {seller_id} 的历史数据行数: {len(product_data)}")
    
    if len(product_data) == 0:
        print(f"警告: 未找到产品 {product_id} 和卖家 {seller_id} 的历史数据")
        return None
    
    # 确保数据按日期排序
    product_data = product_data.sort_values("create_timestamp")
    
    return product_data

def predict_future_sales(product_id, seller_id, unit_price, weeks_ahead):
    """
    预测未来N周的销量
    
    使用产品和卖家的完整历史销售数据进行预测，捕捉所有可能的季节性模式和长期趋势
    """
    global model, product_encoder, seller_encoder, feature_names
    
    # 确保模型已加载
    if model is None:
        load_model_and_encoders()
    
    # 获取历史数据
    historical_sales = get_historical_sales(product_id, seller_id)
    
    if historical_sales is None or len(historical_sales) < 7:
        return {
            "error": f"没有足够的历史数据用于预测。需要至少7天的历史数据。"
        }
    
    # 准备预测结果
    predictions = []
    current_date = historical_sales["create_timestamp"].max()
    
    # 对每一周进行预测
    for week in range(weeks_ahead):
        # 准备特征
        features = prepare_features(
            product_id, 
            seller_id, 
            unit_price, 
            historical_sales,
            current_date + pd.Timedelta(days=7*week)
        )
        
        # 进行预测
        prediction = model.predict(features)[0]
        prediction = max(0, prediction)  # 确保预测值非负
        
        # 添加到预测结果
        predictions.append({
            "week_number": week + 1,
            "prediction_date": str((current_date + pd.Timedelta(days=7*week)).date()),
            "predicted_sales": float(prediction)
        })
    
    return {
        "product_id": product_id,
        "seller_id": seller_id,
        "unit_price": unit_price,
        "predictions": predictions,
        "historical_data_used": {
            "start_date": str(historical_sales["create_timestamp"].min().date()),
            "end_date": str(historical_sales["create_timestamp"].max().date()),
            "total_days": len(historical_sales)
        }
    }

def prepare_features(product_id, seller_id, unit_price, historical_sales, prediction_date):
    """准备模型输入特征"""
    try:
        # 编码product_id和seller_id
        product_id_enc = product_encoder.transform([product_id])[0] if product_id in product_encoder.classes_ else 0
        seller_id_enc = seller_encoder.transform([seller_id])[0] if seller_id in seller_encoder.classes_ else 0
    except:
        product_id_enc = 0
        seller_id_enc = 0
    
    # 创建基础特征
    features = {
        'product_id_enc': product_id_enc,
        'seller_id_enc': seller_id_enc,
        'unit_price': unit_price,
        'dayofweek': prediction_date.dayofweek,
        'day': prediction_date.day,
        'week': prediction_date.isocalendar()[1],
        'month': prediction_date.month,
        'quarter': prediction_date.quarter,
        'year': prediction_date.year,
        'is_weekend': 1 if prediction_date.dayofweek >= 5 else 0,
        'is_month_start': 1 if prediction_date.is_month_start else 0,
        'is_month_end': 1 if prediction_date.is_month_end else 0,
    }
    
    # 添加滞后特征
    sales_series = historical_sales["quantity"].values
    for i in range(1, 8):  # 使用最近7天的数据
        features[f'lag_{i}_quantity'] = sales_series[-i] if len(sales_series) >= i else 0
    
    # 添加移动平均特征
    features['rolling_mean_7d'] = np.mean(sales_series[-7:]) if len(sales_series) >= 7 else np.mean(sales_series)
    features['rolling_std_7d'] = np.std(sales_series[-7:]) if len(sales_series) >= 7 else np.std(sales_series)
    
    if len(sales_series) >= 14:
        features['rolling_mean_14d'] = np.mean(sales_series[-14:])
        features['rolling_std_14d'] = np.std(sales_series[-14:])
    else:
        features['rolling_mean_14d'] = features['rolling_mean_7d']
        features['rolling_std_14d'] = features['rolling_std_7d']
    
    if len(sales_series) >= 30:
        features['rolling_mean_30d'] = np.mean(sales_series[-30:])
        features['rolling_std_30d'] = np.std(sales_series[-30:])
    else:
        features['rolling_mean_30d'] = features['rolling_mean_7d']
        features['rolling_std_30d'] = features['rolling_std_7d']
    
    # 创建特征数组
    X = []
    for feature_name in feature_names:
        if feature_name in features:
            X.append(features[feature_name])
        else:
            X.append(0)
    
    return np.array([X])

def create_app():
    """创建Flask应用程序"""
    app = Flask(__name__)
    
    @app.route('/health', methods=['GET'])
    def health():
        return jsonify({"status": "healthy", "time": str(datetime.datetime.now())})
    
    @app.route('/predict', methods=['POST'])
    def predict_endpoint():
        try:
            input_data = request.json
            product_id = input_data.get('product_id')
            seller_id = input_data.get('seller_id')
            unit_price = input_data.get('unit_price', 0.0)
            weeks_ahead = input_data.get('number_of_week_in_future', 4)  # 默认预测4周
            
            result = predict_future_sales(product_id, seller_id, unit_price, weeks_ahead)
            return jsonify(result)
        except Exception as e:
            return jsonify({"error": str(e)}), 400
    
    return app

def run_server(port=8000):
    """运行Flask服务器"""
    # 确保模型已加载
    load_model_and_encoders()
    
    app = create_app()
    print(f"\n预测API服务启动在: http://localhost:{port}/predict")
    print(f"健康检查端点: http://localhost:{port}/health")
    print("\nAPI使用示例:")
    print("""
curl -X POST http://localhost:8000/predict \\
    -H "Content-Type: application/json" \\
    -d '{
        "product_id": "p100", 
        "seller_id": "seller_1", 
        "unit_price": 99.99,
        "number_of_week_in_future": 4
    }'
    """)
    
    app.run(host='0.0.0.0', port=port)

def main():
    """主函数"""
    parser = argparse.ArgumentParser(description='本地销量预测模型服务')
    parser.add_argument('--server', action='store_true', help='以API服务器模式运行')
    parser.add_argument('--port', type=int, default=8000, help='API服务器端口号')
    parser.add_argument('--product-id', type=str, help='产品ID')
    parser.add_argument('--seller-id', type=str, help='卖家ID')
    parser.add_argument('--unit-price', type=float, default=99.99, help='单价')
    parser.add_argument('--weeks-ahead', type=int, default=4, help='预测未来几周的销量')
    
    args = parser.parse_args()
    
    if args.server:
        run_server(port=args.port)
    else:
        if args.product_id is None or args.seller_id is None:
            print("错误: 必须提供product_id和seller_id")
            return
        
        result = predict_future_sales(
            args.product_id,
            args.seller_id,
            args.unit_price,
            args.weeks_ahead
        )
        print("\n预测结果:")
        print(json.dumps(result, indent=2))

if __name__ == "__main__":
    main() 