#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Seller Sales Simulation and Prediction
Based on test_model.py, analyze a seller's past 30 days sales and predict future 30 days
Generate sales trend charts
"""

import joblib
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from datetime import datetime, timedelta
import warnings
warnings.filterwarnings('ignore')

# Set font for better chart display
plt.rcParams['font.sans-serif'] = ['DejaVu Sans', 'Arial Unicode MS']
plt.rcParams['axes.unicode_minus'] = False

class SellerSalesSimulator:
    def __init__(self, model_path="../model/xgb_sales_predictor.pkl", 
                 data_path="../data/prepared_daily_sales.csv"):
        """Initialize the simulator"""
        self.model = joblib.load(model_path)
        self.data = pd.read_csv(data_path, parse_dates=["date"])
        self.data["create_date"] = self.data["date"].dt.date
        print("✅ Model and data loaded successfully")
        
    def get_seller_info(self):
        """Get seller information"""
        sellers = self.data['seller_id'].unique()
        products = self.data['product_id'].unique()
        print(f"📊 Available sellers: {list(sellers)}")
        print(f"🛍️ Available products: {list(products)}")
        return sellers, products
    
    def get_historical_data(self, seller_id, days=30):
        """Get historical data for specified seller"""
        # Get all data for this seller
        seller_data = self.data[self.data['seller_id'] == seller_id].copy()
        
        if seller_data.empty:
            raise ValueError(f"No data found for seller {seller_id}")
        
        # Get recent date range
        max_date = seller_data['date'].max()
        start_date = max_date - timedelta(days=days)
        
        # Filter last 30 days data
        recent_data = seller_data[
            (seller_data['date'] >= start_date) & 
            (seller_data['date'] <= max_date)
        ].sort_values('date')
        
        print(f"📅 {seller_id} historical data range: {start_date.date()} to {max_date.date()}")
        print(f"📈 Historical data records: {len(recent_data)}")
        
        return recent_data, max_date
    
    def generate_future_features(self, seller_id, last_date, historical_data, days=30):
        """Generate features needed for future predictions"""
        future_predictions = []
        
        # Get product list for this seller
        products = historical_data['product_id'].unique()
        
        # Generate future 30 days predictions for each product
        for product_id in products:
            product_history = historical_data[
                historical_data['product_id'] == product_id
            ].sort_values('date')
            
            if len(product_history) < 3:
                continue
                
            # Get recent prices and other features
            last_record = product_history.iloc[-1]
            
            for day in range(1, days + 1):
                future_date = last_date + timedelta(days=day)
                
                # Basic time features
                is_weekend = 1 if future_date.weekday() >= 5 else 0
                day_of_week = future_date.weekday()
                day_of_month = future_date.day
                month = future_date.month
                
                # Simple holiday detection (weekends as holidays)
                is_holiday = 1 if is_weekend else 0
                
                # Get lag features (from historical data or previous predictions)
                lag_1 = self._get_lag_value(product_history, future_predictions, 
                                          product_id, day, 1)
                lag_7 = self._get_lag_value(product_history, future_predictions, 
                                          product_id, day, 7)
                lag_30 = self._get_lag_value(product_history, future_predictions, 
                                           product_id, day, 30)
                
                # Price features (use recent prices with some random variation)
                price_variation = np.random.normal(0, 0.02)  # 2% price fluctuation
                sale_price = last_record['sale_price'] * (1 + price_variation)
                original_price = last_record['original_price']
                
                future_record = {
                    'date': future_date,
                    'seller_id': seller_id,
                    'product_id': product_id,
                    'sale_price': sale_price,
                    'original_price': original_price,
                    'is_holiday': is_holiday,
                    'is_weekend': is_weekend,
                    'day_of_week': day_of_week,
                    'day_of_month': day_of_month,
                    'month': month,
                    'lag_1': lag_1,
                    'lag_7': lag_7,
                    'lag_30': lag_30
                }
                
                future_predictions.append(future_record)
        
        return pd.DataFrame(future_predictions)
    
    def _get_lag_value(self, history, predictions, product_id, current_day, lag_days):
        """Get lag feature values"""
        if current_day <= lag_days:
            # Get from historical data
            if len(history) >= lag_days:
                return history.iloc[-lag_days]['quantity']
            else:
                return history['quantity'].mean() if len(history) > 0 else 50.0
        else:
            # Get from previous predictions
            target_day = current_day - lag_days
            for pred in predictions:
                if (pred['product_id'] == product_id and 
                    hasattr(pred, 'day_index') and pred['day_index'] == target_day):
                    return pred.get('predicted_quantity', 50.0)
            return 50.0  # Default value
    
    def predict_future_sales(self, future_features):
        """Predict future sales"""
        if future_features.empty:
            return future_features
            
        # Prepare prediction features
        feature_columns = [
            "product_id", "seller_id", "sale_price", "original_price",
            "is_holiday", "is_weekend", "day_of_week", "day_of_month", 
            "month", "lag_1", "lag_7", "lag_30"
        ]
        
        prediction_data = future_features[feature_columns].copy()
        
        # Execute predictions
        predictions = self.model.predict(prediction_data)
        
        # Ensure predictions are positive
        predictions = np.maximum(predictions, 0)
        
        # Add prediction results
        future_features = future_features.copy()
        future_features['predicted_quantity'] = predictions
        
        return future_features
    
    def create_visualization(self, historical_data, future_data, seller_id):
        """Create sales trend visualization"""
        plt.figure(figsize=(15, 10))
        
        # 1. Overall sales trend
        plt.subplot(2, 2, 1)
        
        # Historical data aggregated by date
        hist_daily = historical_data.groupby('date')['quantity'].sum().reset_index()
        future_daily = future_data.groupby('date')['predicted_quantity'].sum().reset_index()
        
        plt.plot(hist_daily['date'], hist_daily['quantity'], 
                'b-', linewidth=2, label='Historical Sales', marker='o')
        plt.plot(future_daily['date'], future_daily['predicted_quantity'], 
                'r--', linewidth=2, label='Predicted Sales', marker='s')
        
        plt.axvline(x=historical_data['date'].max(), color='gray', 
                   linestyle=':', alpha=0.7, label='Prediction Boundary')
        
        plt.title(f'{seller_id} - Daily Sales Trend')
        plt.xlabel('Date')
        plt.ylabel('Sales Quantity')
        plt.legend()
        plt.xticks(rotation=45)
        plt.grid(True, alpha=0.3)
        
        # 2. Sales by product category
        plt.subplot(2, 2, 2)
        
        # Historical data aggregated by product
        hist_product = historical_data.groupby('product_id')['quantity'].sum()
        future_product = future_data.groupby('product_id')['predicted_quantity'].sum()
        
        products = list(set(hist_product.index) | set(future_product.index))
        
        x = np.arange(len(products))
        width = 0.35
        
        hist_values = [hist_product.get(p, 0) for p in products]
        future_values = [future_product.get(p, 0) for p in products]
        
        plt.bar(x - width/2, hist_values, width, label='Historical 30 Days', alpha=0.8)
        plt.bar(x + width/2, future_values, width, label='Predicted 30 Days', alpha=0.8)
        
        plt.title(f'{seller_id} - Sales by Product Comparison')
        plt.xlabel('Product ID')
        plt.ylabel('Total Sales')
        plt.xticks(x, products, rotation=45)
        plt.legend()
        plt.grid(True, alpha=0.3)
        
        # 3. Weekly sales pattern
        plt.subplot(2, 2, 3)
        
        historical_data['weekday'] = historical_data['date'].dt.day_name()
        future_data['weekday'] = future_data['date'].dt.day_name()
        
        hist_weekly = historical_data.groupby('weekday')['quantity'].sum()
        future_weekly = future_data.groupby('weekday')['predicted_quantity'].sum()
        
        weekdays = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday']
        hist_week_values = [hist_weekly.get(d, 0) for d in weekdays]
        future_week_values = [future_weekly.get(d, 0) for d in weekdays]
        
        x = np.arange(len(weekdays))
        plt.bar(x - width/2, hist_week_values, width, label='Historical', alpha=0.8)
        plt.bar(x + width/2, future_week_values, width, label='Predicted', alpha=0.8)
        
        plt.title(f'{seller_id} - Weekly Sales Pattern')
        plt.xlabel('Day of Week')
        plt.ylabel('Total Sales')
        plt.xticks(x, ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'], rotation=45)
        plt.legend()
        plt.grid(True, alpha=0.3)
        
        # 4. Statistics summary
        plt.subplot(2, 2, 4)
        plt.axis('off')
        
        # Calculate statistics
        hist_total = historical_data['quantity'].sum()
        future_total = future_data['predicted_quantity'].sum()
        hist_avg = historical_data['quantity'].mean()
        future_avg = future_data['predicted_quantity'].mean()
        growth_rate = ((future_total - hist_total) / hist_total) * 100
        
        stats_text = f"""
Sales Statistics Summary

Historical 30-day Total Sales: {hist_total:.0f}
Predicted 30-day Total Sales: {future_total:.0f}
Expected Growth Rate: {growth_rate:+.1f}%

Historical Daily Average: {hist_avg:.1f}
Predicted Daily Average: {future_avg:.1f}

Number of Products: {len(historical_data['product_id'].unique())}
Prediction Days: {len(future_data['date'].unique())}
        """
        
        plt.text(0.1, 0.5, stats_text, fontsize=12, 
                verticalalignment='center', transform=plt.gca().transAxes,
                bbox=dict(boxstyle="round,pad=0.3", facecolor="lightgray", alpha=0.5))
        
        plt.tight_layout()
        
        # Save chart
        chart_filename = f"../data/charts/{seller_id}_sales_simulation_{datetime.now().strftime('%Y%m%d_%H%M%S')}.png"
        plt.savefig(chart_filename, dpi=300, bbox_inches='tight')
        print(f"📊 Chart saved: {chart_filename}")
        
        plt.show()
        
        return chart_filename
    
    def run_simulation(self, seller_id="seller_2", historical_days=30, future_days=30):
        """Run complete sales simulation"""
        print(f"🚀 Starting sales simulation for {seller_id}...")
        
        try:
            # 1. Get historical data
            historical_data, last_date = self.get_historical_data(seller_id, historical_days)
            
            # 2. Generate future features
            print("🔮 Generating future prediction features...")
            future_features = self.generate_future_features(
                seller_id, last_date, historical_data, future_days
            )
            
            # 3. Predict future sales
            print("📈 Executing sales predictions...")
            future_predictions = self.predict_future_sales(future_features)
            
            # 4. Create visualization
            print("📊 Generating sales charts...")
            chart_file = self.create_visualization(historical_data, future_predictions, seller_id)
            
            # 5. Output summary
            self.print_summary(historical_data, future_predictions, seller_id)
            
            return historical_data, future_predictions, chart_file
            
        except Exception as e:
            print(f"❌ Error during simulation: {e}")
            return None, None, None
    
    def print_summary(self, historical_data, future_data, seller_id):
        """Print simulation summary"""
        print("\n" + "="*60)
        print(f"📊 {seller_id} Sales Simulation Summary")
        print("="*60)
        
        hist_total = historical_data['quantity'].sum()
        future_total = future_data['predicted_quantity'].sum()
        growth_rate = ((future_total - hist_total) / hist_total) * 100
        
        print(f"📅 Historical Period: {historical_data['date'].min().date()} - {historical_data['date'].max().date()}")
        print(f"🔮 Prediction Period: {future_data['date'].min().date()} - {future_data['date'].max().date()}")
        print(f"📦 Historical Total Sales: {hist_total:.0f}")
        print(f"🎯 Predicted Total Sales: {future_total:.0f}")
        print(f"📈 Expected Growth Rate: {growth_rate:+.1f}%")
        print(f"🛍️ Number of Products: {len(historical_data['product_id'].unique())}")
        
        # Best and worst performing products
        future_by_product = future_data.groupby('product_id')['predicted_quantity'].sum().sort_values(ascending=False)
        print(f"🏆 Best Predicted Product: {future_by_product.index[0]} ({future_by_product.iloc[0]:.0f})")
        print(f"📉 Worst Predicted Product: {future_by_product.index[-1]} ({future_by_product.iloc[-1]:.0f})")


def main():
    """Main function"""
    print("🏪 Seller Sales Simulation and Prediction System")
    print("="*50)
    
    # Create simulator
    simulator = SellerSalesSimulator()
    
    # Show available sellers
    sellers, products = simulator.get_seller_info()
    
    # Select a seller for simulation (can modify here to choose different seller)
    target_seller = "seller_2"  # Can change to seller_1, seller_3, etc.
    
    if target_seller not in sellers:
        print(f"❌ Seller {target_seller} does not exist, using default seller")
        target_seller = sellers[0]
    
    # Run simulation
    historical_data, future_predictions, chart_file = simulator.run_simulation(
        seller_id=target_seller,
        historical_days=30,
        future_days=30
    )
    
    if historical_data is not None:
        print("✅ Simulation completed!")
        print(f"📂 Chart file: {chart_file}")
    else:
        print("❌ Simulation failed")


if __name__ == "__main__":
    main() 