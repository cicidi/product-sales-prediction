#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
销量查询脚本
查询特定seller、product和日期的销量
"""

import pandas as pd
import sys
from datetime import datetime

def query_sales(seller_id, product_id, date_str):
    """
    查询特定seller、product和日期的销量
    
    Args:
        seller_id (str): 卖家ID，例如 'seller_2'
        product_id (str): 产品ID，例如 'p101'  
        date_str (str): 日期，格式 '2024-05-08'
    
    Returns:
        dict: 查询结果
    """
    
    # 读取数据
    try:
        data = pd.read_csv("./data/prepared_daily_sales.csv", parse_dates=["date"])
        print(f"✅ 数据加载成功，共 {len(data):,} 条记录")
    except Exception as e:
        print(f"❌ 数据加载失败: {e}")
        return None
    
    # 转换日期格式
    try:
        target_date = pd.to_datetime(date_str).date()
        data['date'] = pd.to_datetime(data['date']).dt.date
    except Exception as e:
        print(f"❌ 日期格式错误: {e}")
        return None
    
    # 查询筛选
    result = data[
        (data['seller_id'] == seller_id) & 
        (data['product_id'] == product_id) & 
        (data['date'] == target_date)
    ]
    
    print(f"\n🔍 查询条件:")
    print(f"  卖家ID: {seller_id}")
    print(f"  产品ID: {product_id}")
    print(f"  日期: {date_str}")
    
    if len(result) == 0:
        print(f"\n❌ 未找到匹配记录")
        
        # 提供一些建议
        print(f"\n💡 建议检查:")
        
        # 检查该卖家是否存在
        seller_exists = seller_id in data['seller_id'].unique()
        print(f"  卖家 {seller_id} 是否存在: {'✅' if seller_exists else '❌'}")
        
        # 检查该产品是否存在
        product_exists = product_id in data['product_id'].unique()
        print(f"  产品 {product_id} 是否存在: {'✅' if product_exists else '❌'}")
        
        # 检查日期范围
        date_range = f"{data['date'].min()} 到 {data['date'].max()}"
        print(f"  数据日期范围: {date_range}")
        
        # 如果卖家和产品都存在，显示该组合的其他日期
        if seller_exists and product_exists:
            combo_data = data[
                (data['seller_id'] == seller_id) & 
                (data['product_id'] == product_id)
            ]
            if len(combo_data) > 0:
                print(f"\n📅 {seller_id} + {product_id} 的其他销售日期 (前10个):")
                sample_dates = combo_data['date'].head(10).tolist()
                for d in sample_dates:
                    print(f"    {d}")
        
        return None
    
    elif len(result) == 1:
        record = result.iloc[0]
        print(f"\n✅ 找到记录:")
        print(f"  销量: {record['quantity']}")
        print(f"  销售价格: ${record['sale_price']:.2f}")
        print(f"  原价: ${record['original_price']:.2f}")
        print(f"  是否假期: {'是' if record['is_holiday'] else '否'}")
        print(f"  是否周末: {'是' if record['is_weekend'] else '否'}")
        print(f"  滞后特征:")
        print(f"    lag_1: {record['lag_1']}")
        print(f"    lag_7: {record['lag_7']}")
        print(f"    lag_30: {record['lag_30']}")
        
        return record.to_dict()
    
    else:
        print(f"\n⚠️ 找到多条记录 ({len(result)} 条)，这可能表示数据有问题")
        print(result[['date', 'seller_id', 'product_id', 'quantity']])
        return result.to_dict('records')

def main():
    """主函数，支持命令行参数或交互模式"""
    
    if len(sys.argv) == 4:
        # 命令行模式
        seller_id = sys.argv[1]
        product_id = sys.argv[2] 
        date_str = sys.argv[3]
        
        print("🔍 销量查询工具")
        print("=" * 40)
        
        query_sales(seller_id, product_id, date_str)
        
    else:
        # 交互模式
        print("🔍 销量查询工具")
        print("=" * 40)
        
        # 默认查询
        print("执行默认查询: seller_6, p100, 2023-11-30")
        query_sales('seller_5', 'p300', '2023-11-01')
        
        print(f"\n" + "=" * 40)
        print("💡 使用方法:")
        print(f"  python {sys.argv[0]} <seller_id> <product_id> <date>")
        print(f"  例如: python {sys.argv[0]} seller_2 p101 2024-05-08")

if __name__ == "__main__":
    main() 