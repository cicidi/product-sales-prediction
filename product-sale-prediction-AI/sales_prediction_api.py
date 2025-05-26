#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Sales prediction REST API service
Using FastAPI + XGBoost PKL model
Python 3.12 compatible
"""

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from typing import List
import pandas as pd
import joblib
import numpy as np
import os
import sys
from datetime import datetime

# Create FastAPI application
app = FastAPI(
    title="Sales predicate API",
    description=" XGBoost sales prediction API service",
    version="1.0.0"
)

# Global model variable
model = None


# Request model
class PredictionRequest(BaseModel):
  product_id: str = Field(..., example="p101")
  seller_id: str = Field(..., example="seller_2")
  sale_price: float = Field(..., example=899.99)
  original_price: float = Field(..., example=1099.0)
  is_holiday: int = Field(..., example=0)
  is_weekend: int = Field(..., example=1)
  day_of_week: int = Field(..., example=6)
  day_of_month: int = Field(..., example=15)
  month: int = Field(..., example=5)
  lag_1: float = Field(..., example=120.0)
  lag_7: float = Field(..., example=89.0)
  lag_30: float = Field(..., example=95.0)


class PredictionResponse(BaseModel):
  predicted_quantity: float
  status: str = "success"


class BatchRequest(BaseModel):
  requests: List[PredictionRequest]


class BatchResponse(BaseModel):
  predictions: List[float]
  count: int
  status: str = "success"


def get_model_path():
  """load model path"""
  return "/home/cicidi/project/product-sales-prediction/product-sale-prediction-AI/model/xgb_sales_predictor.pkl"


def load_model():
  """load model"""
  global model
  model_path = get_model_path()

  try:
    if not os.path.exists(model_path):
      raise FileNotFoundError(f"model file not exist: {model_path}")

    model = joblib.load(model_path)
    print(f"✅ model load success: {model_path}")
    return True
  except Exception as e:
    print(f"❌ model load failed: {e}")
    return False


def predict_sales(data: pd.DataFrame) -> np.ndarray:
  """Execute prediction"""
  if model is None:
    raise HTTPException(status_code=500, detail="model not loaded")

  try:
    # Ensure feature order
    features = [
      "product_id", "seller_id", "sale_price", "original_price",
      "is_holiday", "is_weekend", "day_of_week", "day_of_month",
      "month", "lag_1", "lag_7", "lag_30"
    ]

    data_ordered = data[features]
    predictions = model.predict(data_ordered)
    return np.maximum(predictions, 0)  #

  except Exception as e:
    raise HTTPException(status_code=500, detail=f"predicate failure: {e}")


# API endpoints

@app.on_event("startup")
async def startup():
  """Load model on startup"""
  print("🚀 start predication API...")
  load_model()


@app.get("/")
async def root():
  """Root path"""
  return {
    "message": "predication API serivce",
    "version": "1.0.0",
    "python_version": sys.version,
    "model_path": get_model_path(),
    "docs": "/docs"
  }


@app.get("/health")
async def health():
  """heath check"""
  return {
    "status": "healthy" if model is not None else "unhealthy",
    "model_loaded": model is not None,
    "model_path": get_model_path(),
    "timestamp": datetime.now().isoformat()
  }


@app.post("/predict", response_model=PredictionResponse)
async def predict_single(request: PredictionRequest):
  """single predication"""
  try:
    # Convert to DataFrame
    data = pd.DataFrame([request.dict()])

    # run prediction
    predictions = predict_sales(data)
    prediction = float(predictions[0])

    return PredictionResponse(predicted_quantity=prediction)

  except Exception as e:
    raise HTTPException(status_code=500, detail=str(e))


@app.post("/predict/batch", response_model=BatchResponse)
async def predict_batch(request: BatchRequest):
  """batch predication"""
  try:
    # Convert to DataFrame
    data_list = [req.dict() for req in request.requests]
    data = pd.DataFrame(data_list)

    # run prediction
    predictions = predict_sales(data)
    predictions_list = [float(p) for p in predictions]

    return BatchResponse(
        predictions=predictions_list,
        count=len(predictions_list)
    )

  except Exception as e:
    raise HTTPException(status_code=500, detail=str(e))


@app.get("/example")
async def get_example():
  """API example"""
  return {
    "single_prediction": {
      "url": "/predict",
      "method": "POST",
      "body": {
        "product_id": "p101",
        "seller_id": "seller_2",
        "sale_price": 899.99,
        "original_price": 1099.0,
        "is_holiday": 0,
        "is_weekend": 1,
        "day_of_week": 6,
        "day_of_month": 15,
        "month": 5,
        "lag_1": 120.0,
        "lag_7": 89.0,
        "lag_30": 95.0
      }
    },
    "curl_example": 'curl -X POST "http://localhost:8000/predict" -H "Content-Type: application/json" -d \'{"product_id": "p101", "seller_id": "seller_2", "sale_price": 899.99, "original_price": 1099.0, "is_holiday": 0, "is_weekend": 1, "day_of_week": 6, "day_of_month": 15, "month": 5, "lag_1": 120.0, "lag_7": 89.0, "lag_30": 95.0}\''
  }


if __name__ == "__main__":
  import uvicorn

  print("🚀 started predication API ...")
  print("📖 API doc: http://localhost:8000/docs")
  print("🔍 health check: http://localhost:8000/health")
  print(f"📁model path: {get_model_path()}")

  uvicorn.run(
      "sales_prediction_api:app",
      host="0.0.0.0",
      port=8000,
      reload=True
  )
