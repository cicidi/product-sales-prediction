import joblib
import pandas as pd
import numpy as np
from sklearn2pmml import sklearn2pmml
from sklearn2pmml.pipeline import PMMLPipeline
from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import StandardScaler
import xgboost as xgb
import os

def convert_model_to_pmml():
    # 模型文件路径
    model_dir = "../product-sale-prediction-AI/train/model"
    model_path = os.path.join(model_dir, "xgb_sales_forecast_model.joblib")
    pmml_path = os.path.join(model_dir, "xgb_sales_forecast_model.pmml")
    
    try:
        # 加载模型
        print("Loading XGBoost model...")
        xgb_model = joblib.load(model_path)
        
        # 打印模型信息
        print("\nModel Information:")
        print(f"Model type: {type(xgb_model)}")
        print(f"Model parameters: {xgb_model.get_params()}")
        print(f"Number of features used by model: {xgb_model.n_features_in_}")
        
        # 使用模型的原始特征名称
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
        
        print(f"\nUsing {len(feature_names)} features: {feature_names}")
        
        # 创建示例数据
        print("\nCreating example data...")
        X = pd.DataFrame(np.random.rand(100, len(feature_names)), columns=feature_names)
        y = np.random.rand(100)  # 示例目标变量
        
        # 创建预处理器
        print("Creating preprocessor...")
        preprocessor = ColumnTransformer(
            transformers=[
                ('num', StandardScaler(), feature_names)
            ],
            remainder='passthrough'
        )
        
        # 创建新的XGBoost模型，使用原始模型的参数
        print("Creating new XGBoost model...")
        new_xgb = xgb.XGBRegressor(
            n_estimators=xgb_model.n_estimators,
            max_depth=xgb_model.max_depth,
            learning_rate=xgb_model.learning_rate,
            objective='reg:squarederror',
            colsample_bytree=0.8,
            subsample=0.8,
            min_child_weight=3,
            random_state=42
        )
        
        # 创建并训练pipeline
        print("Creating and training pipeline...")
        pipeline = PMMLPipeline([
            ("preprocessor", preprocessor),
            ("regressor", new_xgb)
        ])
        
        # 训练pipeline
        print("Fitting pipeline...")
        pipeline.fit(X, y)
        
        # 复制原始模型的参数到新模型
        print("Copying model parameters...")
        new_xgb._Booster = xgb_model._Booster
        
        # 转换为PMML
        print("Converting to PMML...")
        sklearn2pmml(pipeline, pmml_path, with_repr=True)
        
        print(f"Successfully converted model to PMML format: {pmml_path}")
        return True
        
    except Exception as e:
        print(f"Error converting model to PMML: {str(e)}")
        return False

if __name__ == "__main__":
    convert_model_to_pmml() 