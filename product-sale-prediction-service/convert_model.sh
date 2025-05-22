#!/bin/bash

# 检查Python环境
if ! command -v python3 &> /dev/null; then
    echo "Python3 is required but not found. Please install Python3 first."
    exit 1
fi

# 创建虚拟环境
echo "Creating Python virtual environment..."
python3 -m venv venv

# 激活虚拟环境
echo "Activating virtual environment..."
source venv/bin/activate

# 安装必要的依赖
echo "Installing required packages..."
pip install --upgrade pip
pip install joblib pandas numpy 'scikit-learn==1.5.2' xgboost sklearn2pmml sklearn-pandas

# 生成编码文件
echo "Generating encoding files..."
python generate_encodings.py

# 运行转换脚本
echo "Running model conversion script..."
python convert_model_to_pmml.py

# 检查转换结果
if [ $? -eq 0 ]; then
    echo "Model conversion completed successfully."
else
    echo "Model conversion failed."
    exit 1
fi

# 停用虚拟环境
deactivate

echo "Done." 