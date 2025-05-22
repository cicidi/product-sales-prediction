#!/bin/bash

# 该脚本运行模型转换脚本，将XGBoost模型从joblib格式转换为PMML格式

# 获取当前脚本目录的绝对路径
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

# 模型目录和输出目录
MODEL_DIR="../../../product-sale-prediction-AI/train/model"
OUTPUT_DIR="$MODEL_DIR"

# 检查Python环境
if ! command -v python3 &> /dev/null; then
  echo "错误: 需要Python 3，但未找到"
  exit 1
fi

# 安装必要的Python包
echo "检查必要的Python包..."
python3 -m pip install -q joblib pandas numpy scikit-learn sklearn2pmml

# 运行转换脚本
echo "运行模型转换脚本..."
cd "$SCRIPT_DIR" && python3 convert_model_to_pmml.py --model-dir "$MODEL_DIR" --output-dir "$OUTPUT_DIR"

# 检查脚本运行结果
if [ $? -eq 0 ]; then
  echo "模型成功转换为PMML格式"
  echo "现在可以使用Java MLModelService直接加载和使用模型了"
else
  echo "模型转换失败"
  exit 1
fi 