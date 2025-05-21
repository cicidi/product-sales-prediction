import pandas as pd
import numpy as np
import xgboost as xgb
import joblib
import os
import sys
import argparse
from sklearn.preprocessing import LabelEncoder
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_squared_error, mean_absolute_error, r2_score

# 定义SageMaker路径常量
# 当在SageMaker上运行时，这些路径将是标准的
PREFIX = '/opt/ml/'
INPUT_PATH = PREFIX + 'input/data'
OUTPUT_PATH = os.path.join(PREFIX, 'output')
MODEL_PATH = os.path.join(PREFIX, 'model')
TRAIN_PATH = os.path.join(INPUT_PATH, 'train')
VALIDATION_PATH = os.path.join(INPUT_PATH, 'validation')

# 定义默认数据文件路径
DEFAULT_DATA_PATH = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 
                                "resources", 
                                "sales_2023_2025_realistic.csv")

def load_data(file_path=None):
    """加载数据并进行初步处理"""
    # 如果未指定文件路径，使用默认路径
    if file_path is None or not os.path.exists(file_path):
        file_path = DEFAULT_DATA_PATH
        
    print(f"加载数据: {file_path}")
    
    # 加载数据
    df = pd.read_csv(file_path, parse_dates=["create_timestamp"])
    df["create_timestamp"] = pd.to_datetime(df["create_timestamp"])
    return df

def create_time_features(df):
    """创建时间相关特征"""
    # 基础时间特征
    df["dayofweek"] = df["create_timestamp"].dt.dayofweek
    df["day"] = df["create_timestamp"].dt.day
    df["week"] = df["create_timestamp"].dt.isocalendar().week
    df["month"] = df["create_timestamp"].dt.month
    df["quarter"] = df["create_timestamp"].dt.quarter
    df["year"] = df["create_timestamp"].dt.year
    df["is_weekend"] = df["dayofweek"].isin([5, 6]).astype(int)
    df["is_month_start"] = df["create_timestamp"].dt.is_month_start.astype(int)
    df["is_month_end"] = df["create_timestamp"].dt.is_month_end.astype(int)
    
    return df

def aggregate_daily_sales(df):
    """聚合每日销量数据"""
    # 按product_id, seller_id和日期聚合
    daily_sales = df.groupby(["product_id", "seller_id", pd.Grouper(key="create_timestamp", freq="D")])
    
    # 聚合销量和价格
    agg_data = daily_sales.agg({
        "quantity": "sum",  # 总销量
        "unit_price": "mean"  # 平均价格
    }).reset_index()
    
    # 获取日期特征
    agg_data = create_time_features(agg_data)
    
    # 按product_id和seller_id排序
    return agg_data.sort_values(["product_id", "seller_id", "create_timestamp"])

def create_lag_features(df, target_col="quantity", n_lags=7):
    """创建滞后特征"""
    group_cols = ["product_id", "seller_id"]
    
    # 为每个产品/卖家组合创建滞后特征
    for lag in range(1, n_lags + 1):
        df[f"lag_{lag}_{target_col}"] = df.groupby(group_cols)[target_col].shift(lag)
    
    # 创建移动平均特征
    for window in [7, 14, 30]:
        df[f"rolling_mean_{window}d"] = df.groupby(group_cols)[target_col].transform(
            lambda x: x.shift(1).rolling(window=window, min_periods=1).mean()
        )
        df[f"rolling_std_{window}d"] = df.groupby(group_cols)[target_col].transform(
            lambda x: x.shift(1).rolling(window=window, min_periods=1).std()
        )
    
    return df

def create_target(df, forecast_horizon=7):
    """创建目标变量: 未来n天的销量"""
    group_cols = ["product_id", "seller_id"]
    
    # 预测未来forecast_horizon天的销量
    df[f"target_sales_{forecast_horizon}d"] = df.groupby(group_cols)["quantity"].transform(
        lambda x: x.shift(-forecast_horizon)
    )
    
    # 或者，预测未来forecast_horizon天的累计销量
    df[f"target_sales_next_{forecast_horizon}d_sum"] = df.groupby(group_cols)["quantity"].transform(
        lambda x: x.shift(-1).rolling(forecast_horizon).sum()
    )
    
    return df

def encode_categorical_features(df):
    """编码分类特征"""
    encoders = {}
    
    # 对分类特征进行编码
    for col in ["product_id", "seller_id"]:
        le = LabelEncoder()
        df[f"{col}_enc"] = le.fit_transform(df[col])
        encoders[col] = le
    
    return df, encoders

def prepare_train_features(df, target_column="target_sales_next_7d_sum"):
    """准备训练特征"""
    # 数值特征
    num_features = [
        "unit_price", "dayofweek", "day", "week", "month", "quarter", "year",
        "is_weekend", "is_month_start", "is_month_end", 
        "product_id_enc", "seller_id_enc"
    ]
    
    # 添加滞后特征
    lag_features = [col for col in df.columns if col.startswith("lag_") or col.startswith("rolling_")]
    
    # 合并所有特征
    features = num_features + lag_features
    
    # 移除含有NaN的行
    df_clean = df.dropna(subset=features + [target_column])
    
    # 输出剩余行数
    print(f"数据清洗后剩余行数: {len(df_clean)}")
    
    # 准备特征和标签
    X = df_clean[features]
    y = df_clean[target_column]
    
    return X, y, features

def train_model(X_train, y_train, X_test, y_test):
    """训练XGBoost模型"""
    print("训练XGBoost模型...")
    
    # 定义模型参数
    params = {
        'objective': 'reg:squarederror',
        'max_depth': 6,
        'learning_rate': 0.1,
        'n_estimators': 100,
        'subsample': 0.8,
        'colsample_bytree': 0.8,
        'min_child_weight': 3,
        'n_jobs': -1,
        'random_state': 42
    }
    
    # 训练模型 - 简化的fit调用以避免版本兼容性问题
    model = xgb.XGBRegressor(**params)
    try:
        # 尝试添加验证集和早停
        model.fit(
            X_train, y_train,
            eval_set=[(X_test, y_test)],
            verbose=True
        )
    except TypeError:
        print("备用训练方法: 移除了eval_set参数")
        # 如果上面的方法不起作用，使用最基本的fit
        model.fit(X_train, y_train)
    
    return model

def evaluate_model(model, X_test, y_test):
    """评估模型性能"""
    # 预测测试集
    y_pred = model.predict(X_test)
    
    # 计算评估指标
    rmse = np.sqrt(mean_squared_error(y_test, y_pred))
    mae = mean_absolute_error(y_test, y_pred)
    r2 = r2_score(y_test, y_pred)
    
    print(f"测试集RMSE: {rmse:.2f}")
    print(f"测试集MAE: {mae:.2f}")
    print(f"测试集R²: {r2:.4f}")
    
    return rmse, mae, r2

def save_model(model, encoders, feature_names, output_dir='model'):
    """保存模型和编码器"""
    print(f"保存模型到: {output_dir}")
    
    # 确保输出目录存在
    os.makedirs(output_dir, exist_ok=True)
    
    # 保存模型
    model_path = os.path.join(output_dir, "xgb_sales_forecast_model.joblib")
    joblib.dump(model, model_path)
    
    # 保存编码器
    for name, encoder in encoders.items():
        encoder_path = os.path.join(output_dir, f"label_encoder_{name}.joblib")
        joblib.dump(encoder, encoder_path)
    
    # 保存特征名称
    feature_path = os.path.join(output_dir, "feature_names.joblib")
    joblib.dump(feature_names, feature_path)
    
    print("模型和处理器保存完成")

def main():
    """主函数"""
    parser = argparse.ArgumentParser()
    
    # 参数配置
    parser.add_argument('--train-file', type=str, default=DEFAULT_DATA_PATH,
                        help='训练数据文件路径')
    parser.add_argument('--forecast-horizon', type=int, default=7,
                        help='预测未来天数')
    parser.add_argument('--lag-days', type=int, default=14,
                        help='使用过去多少天的滞后特征')
    
    # 解析参数
    args, _ = parser.parse_known_args()
    
    # 加载数据
    df = load_data(args.train_file)
    
    # 数据探索
    print(f"原始数据形状: {df.shape}")
    print(f"数据列: {df.columns.tolist()}")
    
    # 按日期聚合数据
    daily_sales = aggregate_daily_sales(df)
    print(f"聚合后数据形状: {daily_sales.shape}")
    
    # 创建滞后特征
    daily_sales = create_lag_features(daily_sales, n_lags=args.lag_days)
    
    # 创建目标变量
    daily_sales = create_target(daily_sales, forecast_horizon=args.forecast_horizon)
    
    # 编码分类特征
    daily_sales, encoders = encode_categorical_features(daily_sales)
    
    # 准备特征和目标
    target_col = f"target_sales_next_{args.forecast_horizon}d_sum"
    X, y, feature_names = prepare_train_features(daily_sales, target_column=target_col)
    
    # 分割训练集和测试集
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, shuffle=False, random_state=42
    )
    
    # 训练模型
    model = train_model(X_train, y_train, X_test, y_test)
    
    # 评估模型
    evaluate_model(model, X_test, y_test)
    
    # 确定模型输出路径
    output_dir = MODEL_PATH if os.path.exists(PREFIX) else 'model'
    
    # 保存模型
    save_model(model, encoders, feature_names, output_dir)
    
    print("训练完成! ✅")

if __name__ == "__main__":
    main()
