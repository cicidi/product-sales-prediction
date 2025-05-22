#!/usr/bin/env python3
"""
转换XGBoost模型从joblib格式到PMML格式
这样Java应用程序可以直接加载和使用模型
"""

import os
import sys
import joblib
import pandas as pd
import numpy as np
from sklearn2pmml import sklearn2pmml
from sklearn2pmml.pipeline import PMMLPipeline
from sklearn.preprocessing import LabelEncoder
import argparse

def convert_model_to_pmml(model_dir, output_dir=None):
    """
    将joblib格式的XGBoost模型转换为PMML格式
    
    Args:
        model_dir: 模型文件目录
        output_dir: 输出目录，默认与model_dir相同
    """
    if output_dir is None:
        output_dir = model_dir
    
    # 检查目录是否存在
    if not os.path.exists(model_dir):
        raise ValueError(f"模型目录不存在: {model_dir}")
    
    os.makedirs(output_dir, exist_ok=True)
    
    # 加载模型和编码器
    model_path = os.path.join(model_dir, "xgb_sales_forecast_model.joblib")
    product_encoder_path = os.path.join(model_dir, "label_encoder_product_id.joblib")
    seller_encoder_path = os.path.join(model_dir, "label_encoder_seller_id.joblib")
    feature_names_path = os.path.join(model_dir, "feature_names.joblib")
    
    print(f"加载模型: {model_path}")
    model = joblib.load(model_path)
    
    print(f"加载产品编码器: {product_encoder_path}")
    product_encoder = joblib.load(product_encoder_path)
    
    print(f"加载卖家编码器: {seller_encoder_path}")
    seller_encoder = joblib.load(seller_encoder_path)
    
    print(f"加载特征名称: {feature_names_path}")
    feature_names = joblib.load(feature_names_path)
    
    # 创建PMML管道
    pipeline = PMMLPipeline([
        ("model", model)
    ])
    
    # 设置特征名称
    pipeline.configure(features=feature_names)
    
    # 导出PMML文件
    pmml_path = os.path.join(output_dir, "xgb_sales_forecast_model.pmml")
    print(f"导出PMML文件: {pmml_path}")
    sklearn2pmml(pipeline, pmml_path)
    
    # 导出编码映射到CSV文件，供Java加载使用
    export_encodings_to_csv(product_encoder, "product_id", output_dir)
    export_encodings_to_csv(seller_encoder, "seller_id", output_dir)
    
    # 导出特征名称到文本文件
    feature_names_txt_path = os.path.join(output_dir, "feature_names.txt")
    with open(feature_names_txt_path, 'w') as f:
        for feature in feature_names:
            f.write(f"{feature}\n")
    
    print("转换完成!")
    print(f"模型已导出到: {pmml_path}")
    print(f"产品ID编码已导出到: {os.path.join(output_dir, 'product_id_encodings.csv')}")
    print(f"卖家ID编码已导出到: {os.path.join(output_dir, 'seller_id_encodings.csv')}")
    print(f"特征名称已导出到: {feature_names_txt_path}")

def export_encodings_to_csv(encoder, name, output_dir):
    """
    导出LabelEncoder的编码映射到CSV文件
    
    Args:
        encoder: LabelEncoder对象
        name: 编码器名称 (product_id 或 seller_id)
        output_dir: 输出目录
    """
    encodings = {}
    for i, class_value in enumerate(encoder.classes_):
        encodings[class_value] = i
    
    # 导出到CSV
    output_path = os.path.join(output_dir, f"{name}_encodings.csv")
    with open(output_path, 'w') as f:
        for key, value in encodings.items():
            f.write(f"{key},{value}\n")
    
    print(f"已导出{len(encodings)}个{name}编码映射到{output_path}")

def main():
    parser = argparse.ArgumentParser(description='转换XGBoost模型从joblib格式到PMML格式')
    parser.add_argument('--model-dir', type=str, default='../product-sale-prediction-AI/train/model',
                        help='模型文件目录')
    parser.add_argument('--output-dir', type=str, default=None,
                        help='输出目录，默认与model_dir相同')
    
    args = parser.parse_args()
    try:
        convert_model_to_pmml(args.model_dir, args.output_dir)
    except Exception as e:
        print(f"错误: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main() 