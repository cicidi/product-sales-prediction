import os
import json
import joblib
import numpy as np
import pandas as pd

# 模型和处理器文件路径
MODEL_PATH = os.path.join(os.environ.get('SM_MODEL_DIR', '/opt/ml/model'), 'model/xgb_sales_forecast_model.joblib')
PRODUCT_ENCODER_PATH = os.path.join(os.environ.get('SM_MODEL_DIR', '/opt/ml/model'), 'model/label_encoder_product_id.joblib')
SELLER_ENCODER_PATH = os.path.join(os.environ.get('SM_MODEL_DIR', '/opt/ml/model'), 'model/label_encoder_seller_id.joblib')
FEATURE_NAMES_PATH = os.path.join(os.environ.get('SM_MODEL_DIR', '/opt/ml/model'), 'model/feature_names.joblib')

# 加载模型和处理器
model = None
product_encoder = None
seller_encoder = None
feature_names = None

def model_fn(model_dir):
    """
    加载模型和相关处理器
    """
    global model, product_encoder, seller_encoder, feature_names
    
    print(f"Loading model from {MODEL_PATH}")
    model = joblib.load(MODEL_PATH)
    
    print(f"Loading product encoder from {PRODUCT_ENCODER_PATH}")
    product_encoder = joblib.load(PRODUCT_ENCODER_PATH)
    
    print(f"Loading seller encoder from {SELLER_ENCODER_PATH}")
    seller_encoder = joblib.load(SELLER_ENCODER_PATH)
    
    print(f"Loading feature names from {FEATURE_NAMES_PATH}")
    feature_names = joblib.load(FEATURE_NAMES_PATH)
    
    return model

def input_fn(request_body, request_content_type):
    """
    解析输入数据
    """
    if request_content_type == 'application/json':
        input_data = json.loads(request_body)
        return input_data
    else:
        raise ValueError(f"不支持的内容类型: {request_content_type}, 只支持 'application/json'")

def preprocess(input_data):
    """
    预处理输入数据，将其转换为模型可接受的格式
    """
    # 提取必要信息
    product_id = input_data.get('product_id')
    seller_id = input_data.get('seller_id')
    unit_price = input_data.get('unit_price', 0.0)
    
    # 获取历史销量数据
    historical_sales = input_data.get('historical_sales', [])
    
    # 确保有足够的历史销量数据
    if len(historical_sales) < 7:
        raise ValueError(f"需要至少7天的历史销量数据，但只提供了 {len(historical_sales)} 天")
    
    try:
        # 编码product_id和seller_id
        product_id_enc = product_encoder.transform([product_id])[0] if product_id in product_encoder.classes_ else -1
        seller_id_enc = seller_encoder.transform([seller_id])[0] if seller_id in seller_encoder.classes_ else -1
        
        # 如果产品或卖家ID未知，使用合理的替代值
        if product_id_enc == -1 or seller_id_enc == -1:
            print(f"警告：未知的product_id ({product_id}) 或 seller_id ({seller_id})，使用替代值")
    except:
        # 如果编码失败，使用安全默认值
        product_id_enc = 0
        seller_id_enc = 0
        print(f"警告：编码 product_id ({product_id}) 或 seller_id ({seller_id}) 失败，使用默认值")
    
    # 获取当前日期 (假设是当天预测)
    current_date = pd.Timestamp.now()
    
    # 创建特征字典
    features = {
        'product_id_enc': product_id_enc,
        'seller_id_enc': seller_id_enc,
        'unit_price': unit_price,
        'dayofweek': current_date.dayofweek,
        'day': current_date.day,
        'week': current_date.isocalendar()[1],  # ISO日历周
        'month': current_date.month,
        'quarter': current_date.quarter,
        'year': current_date.year,
        'is_weekend': 1 if current_date.dayofweek >= 5 else 0,
        'is_month_start': 1 if current_date.is_month_start else 0,
        'is_month_end': 1 if current_date.is_month_end else 0,
    }
    
    # 添加滞后特征 (最近7天销量)
    for i, sales in enumerate(historical_sales[:7]):
        features[f'lag_{i+1}_quantity'] = sales
    
    # 添加移动平均特征
    sales_array = np.array(historical_sales)
    features['rolling_mean_7d'] = np.mean(sales_array[:7]) if len(sales_array) >= 7 else np.mean(sales_array)
    features['rolling_std_7d'] = np.std(sales_array[:7]) if len(sales_array) >= 7 else np.std(sales_array)
    
    # 处理更大窗口的移动平均，如果有足够数据
    if len(sales_array) >= 14:
        features['rolling_mean_14d'] = np.mean(sales_array[:14])
        features['rolling_std_14d'] = np.std(sales_array[:14])
    else:
        features['rolling_mean_14d'] = features['rolling_mean_7d']
        features['rolling_std_14d'] = features['rolling_std_7d']
    
    if len(sales_array) >= 30:
        features['rolling_mean_30d'] = np.mean(sales_array[:30])
        features['rolling_std_30d'] = np.std(sales_array[:30])
    else:
        features['rolling_mean_30d'] = features['rolling_mean_7d']
        features['rolling_std_30d'] = features['rolling_std_7d']
    
    # 创建特征数组，确保与训练时相同顺序
    X = []
    for feature_name in feature_names:
        if feature_name in features:
            X.append(features[feature_name])
        else:
            # 如果特征缺失，填充0
            print(f"警告：缺失特征 {feature_name}，填充0")
            X.append(0)
    
    return np.array([X])

def predict_fn(input_data, model):
    """
    使用模型进行预测
    """
    try:
        # 预处理输入数据
        features = preprocess(input_data)
        
        # 使用模型预测
        predictions = model.predict(features)
        
        # 确保预测值非负
        prediction = float(max(0, predictions[0]))
        
        # 返回预测结果
        return {
            'prediction': prediction,
            'product_id': input_data.get('product_id'),
            'seller_id': input_data.get('seller_id')
        }
    except Exception as e:
        # 返回错误信息
        return {
            'error': str(e),
            'product_id': input_data.get('product_id', 'unknown'),
            'seller_id': input_data.get('seller_id', 'unknown')
        }

def output_fn(prediction, accept):
    """
    格式化输出
    """
    if accept == 'application/json':
        return json.dumps(prediction), accept
    else:
        return json.dumps(prediction), 'application/json' 