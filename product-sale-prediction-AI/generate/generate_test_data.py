#!/usr/bin/env python3
"""
Sales Data Generator

Generates realistic sales test data according to sales_data_specification.md
Outputs 100,000 order records to data/sales_2023_2025_realistic.csv
"""

import pandas as pd
import numpy as np
import uuid
from datetime import datetime, timedelta
import os
import random
from typing import List, Dict, Tuple

# Set random seed for reproducible results
np.random.seed(42)
random.seed(42)

class SalesDataGenerator:
    def __init__(self):
        self.start_date = datetime(2023, 1, 1)
        self.end_date = datetime(2025, 5, 24)
        self.total_orders = 10000000  # 10 million orders for realistic scale
        
        # Product data (matching final_sample_products.csv)
        self.products = [
            {"id": "p100", "name": "Apple iPhone 14 128GB", "category": "electronics", "brand": "Apple", "price": 799.0},
            {"id": "p101", "name": "Apple iPhone 15 Pro 256GB", "category": "electronics", "brand": "Apple", "price": 1099.0},
            {"id": "p102", "name": "Apple iPhone 16 Pro Max 512GB", "category": "electronics", "brand": "Apple", "price": 1299.0},
            {"id": "p200", "name": "Samsung Galaxy S24 Ultra", "category": "electronics", "brand": "Samsung", "price": 1099.99},
            {"id": "p201", "name": "Google Pixel 8 Pro", "category": "electronics", "brand": "Google", "price": 999.99},
            {"id": "p300", "name": "Nike Air Zoom Pegasus 40", "category": "clothes", "brand": "Nike", "price": 130.0},
            {"id": "p301", "name": "Uniqlo Ultra Light Down Jacket", "category": "clothes", "brand": "Uniqlo", "price": 79.9},
            {"id": "p302", "name": "Adidas 3-Stripes T-Shirt", "category": "clothes", "brand": "Adidas", "price": 25.0},
            {"id": "p400", "name": "Kirkland Organic Almond Butter", "category": "food", "brand": "Kirkland", "price": 12.99},
            {"id": "p401", "name": "Blue Diamond Whole Natural Almonds", "category": "food", "brand": "Blue Diamond", "price": 10.49},
            {"id": "p402", "name": "Lindt Swiss Chocolate Assorted", "category": "food", "brand": "Lindt", "price": 15.99}
        ]
        
        # Seller data
        self.sellers = ["seller_1", "seller_2", "seller_3", "seller_4", "seller_5", "seller_6"]
        
        # Holidays (major US holidays)
        self.holidays = self._generate_holidays()
        
        # Product-seller mapping (each product sold by 2-3 sellers)
        self.product_seller_mapping = self._create_product_seller_mapping()
        
    def _generate_holidays(self) -> List[datetime]:
        """Generate major US holidays for 2023-2025"""
        holidays = []
        for year in [2023, 2024, 2025]:
            holidays.extend([
                datetime(year, 1, 1),   # New Year's Day
                datetime(year, 7, 4),   # Independence Day
                datetime(year, 12, 25), # Christmas
                datetime(year, 11, 23), # Thanksgiving (approximate)
                datetime(year, 2, 14),  # Valentine's Day
                datetime(year, 10, 31), # Halloween
            ])
        return holidays
    
    def _create_product_seller_mapping(self) -> Dict[str, List[str]]:
        """Assign 2-3 sellers for each product, ensuring all sellers have products"""
        mapping = {}
        
        # First, ensure seller_1 (major seller) sells all products
        for product in self.products:
            mapping[product["id"]] = ["seller_1"]
        
        # Then assign other sellers to ensure each seller gets at least some products
        other_sellers = self.sellers[1:]  # seller_2 to seller_6
        products_list = [p["id"] for p in self.products]
        
        # Distribute products among other sellers more evenly
        for i, seller in enumerate(other_sellers):
            # Each seller gets assigned to specific products
            for j, product_id in enumerate(products_list):
                # Use different modulo patterns to ensure better distribution
                # Each seller gets roughly 50-70% of products
                if (j + i * 2) % 3 != 0:  # Different pattern for each seller
                    if seller not in mapping[product_id]:
                        mapping[product_id].append(seller)
        
        # Ensure each product has at least 2 sellers and each seller has at least some products
        seller_product_count = {seller: 0 for seller in self.sellers}
        
        # Count current assignments
        for product_id, sellers in mapping.items():
            for seller in sellers:
                seller_product_count[seller] += 1
        
        # If any seller has no products, assign them some
        for seller in other_sellers:
            if seller_product_count[seller] == 0:
                # Assign this seller to a few random products
                available_products = random.sample(products_list, min(3, len(products_list)))
                for product_id in available_products:
                    if len(mapping[product_id]) < 3:  # Don't exceed 3 sellers per product
                        mapping[product_id].append(seller)
                        seller_product_count[seller] += 1
        
        return mapping
    
    def _is_holiday(self, date: datetime) -> bool:
        """Check if date is a holiday"""
        return any(abs((date - holiday).days) <= 1 for holiday in self.holidays)
    
    def _is_weekend(self, date: datetime) -> bool:
        """Check if date is weekend"""
        return date.weekday() >= 5  # 5=Saturday, 6=Sunday
    
    def _calculate_discount_factor(self, product_id: str, date: datetime) -> float:
        """Calculate discount factor (between 0.80-1.0, max 20% discount)"""
        base_discount = 1.0  # Start with no discount
        
        # Large holiday discount (max 20%)
        if self._is_holiday(date):
            base_discount -= random.uniform(0.10, 0.20)  # 10-20% holiday discount
        
        # Weekend discount (max 20%)
        elif self._is_weekend(date):
            if any(p["id"] == product_id and p["category"] == "food" for p in self.products):
                base_discount -= random.uniform(0.08, 0.20)  # Food weekend discount 8-20%
            else:
                base_discount -= random.uniform(0.05, 0.15)  # Other categories weekend discount 5-15%
        
        # Regular weekday discounts (smaller, optional)
        else:
            # 70% chance of having some discount on weekdays
            if random.random() < 0.7:
                base_discount -= random.uniform(0.02, 0.10)  # Small weekday discount 2-10%
            # 30% chance of no discount at all
        
        # End-of-month clearance (additional discount)
        if date.day >= 25:
            base_discount -= random.uniform(0.02, 0.08)  # 2-8% additional end-of-month discount
        
        # Add small random fluctuation (±3%)
        base_discount += random.uniform(-0.03, 0.03)
        
        # Ensure discount never exceeds 20% (minimum price factor is 0.80)
        # Some products may have no discount at all (price factor = 1.0)
        return max(0.80, min(1.0, base_discount))
    
    def _calculate_quantity(self, product_id: str, seller_id: str, unit_price: float, 
                          original_price: float, date: datetime) -> int:
        """Calculate order quantity (core logic)"""
        
        # Base quantity (by product category)
        product = next(p for p in self.products if p["id"] == product_id)
        category = product["category"]
        
        if category == "electronics":
            base_qty = 1.5
        elif category == "clothes":
            base_qty = 2.0
        else:  # food
            base_qty = 3.0
        
        # Price influence (adjust for smaller discounts)
        discount_ratio = (original_price - unit_price) / original_price
        price_multiplier = 1.0 + (discount_ratio * 3.0)  # Increase sensitivity since discounts are smaller
        
        # Seller influence (reduced seller effect)
        seller_multiplier = 1.15 if seller_id == "seller_1" else 1.0  # Reduce to 15%
        
        # Time influence (more realistic settings)
        time_multiplier = 1.0
        
        # Holiday promotions (reduced holiday impact)
        if self._is_holiday(date) and category in ["electronics", "clothes"]:
            time_multiplier *= 1.15  # Reduce to 15% impact
        
        # Weekend effect (more realistic weekend effect)
        if self._is_weekend(date):
            if category == "food":
                time_multiplier *= 1.08  # Food weekend increase 8%
            elif category == "electronics":
                time_multiplier *= 1.05  # Electronics weekend increase 5%
            else:  # clothes
                time_multiplier *= 1.03  # Clothes weekend increase 3%
        
        # Beginning-of-month payday effect (reduced month-start effect)
        if date.day <= 5:
            time_multiplier *= 1.05  # Reduce to 5% impact
        
        # Seasonal influence (reduced seasonal effect)
        month = date.month
        if category == "clothes":
            # Seasonal clothing sales increase
            if month in [3, 4, 9, 10]:  # Spring/Fall season change
                time_multiplier *= 1.10  # Reduce to 10% impact
        elif category == "electronics":
            # Year-end electronics sales increase
            if month in [11, 12]:
                time_multiplier *= 1.15  # Reduce to 15% impact
        
        # Calculate final quantity
        final_qty = base_qty * price_multiplier * seller_multiplier * time_multiplier
        
        # Add randomness and ensure reasonable range
        final_qty *= random.uniform(0.8, 1.2)
        
        # Use Poisson distribution and limit range
        qty = np.random.poisson(max(1, final_qty))
        
        # Ensure balanced quantities across different products
        if category == "electronics":
            qty = min(qty, 5)  # Electronics limited to lower quantities
        elif category == "clothes":
            qty = min(qty, 8)  # Clothes medium quantities
        else:  # food
            qty = min(qty, 15)  # Food can have higher quantities
        
        return max(1, qty)
    
    def generate_orders(self) -> pd.DataFrame:
        """Generate order data"""
        orders = []
        
        print(f"Generating {self.total_orders} order records...")
        
        for i in range(self.total_orders):
            if i % 10000 == 0:
                print(f"Progress: {i}/{self.total_orders}")
            
            # Random time selection
            random_days = random.randint(0, (self.end_date - self.start_date).days)
            order_date = self.start_date + timedelta(days=random_days)
            
            # Random product selection
            product = random.choice(self.products)
            product_id = product["id"]
            original_price = product["price"]
            
            # Select available seller for this product
            available_sellers = self.product_seller_mapping[product_id]
            seller_id = random.choice(available_sellers)
            
            # Calculate discounted price
            discount_factor = self._calculate_discount_factor(product_id, order_date)
            unit_price = round(original_price * discount_factor, 2)
            
            # Calculate quantity
            quantity = self._calculate_quantity(product_id, seller_id, unit_price, original_price, order_date)
            
            # Calculate subtotal
            subtotal = round(unit_price * quantity, 2)
            
            # Generate random buyer ID
            buyer_id = f"buyer_{random.randint(1, 10000)}"
            
            # Format timestamp as required (yyyy/MM/dd HH:MM:SS)
            timestamp_str = order_date.strftime("%Y/%m/%d %H:%M:%S")
            
            # Add random hour and minute
            hour = random.randint(0, 23)
            minute = random.randint(0, 59)
            second = random.randint(0, 59)
            full_timestamp = order_date.replace(hour=hour, minute=minute, second=second)
            timestamp_str = full_timestamp.strftime("%Y/%m/%d %H:%M:%S")
            
            order = {
                'order_id': str(uuid.uuid4()),
                'product_id': product_id,
                'buyer_id': buyer_id,
                'seller_id': seller_id,
                'unit_price': unit_price,
                'quantity': quantity,
                'subtotal': subtotal,
                'create_timestamp': timestamp_str
            }
            
            orders.append(order)
        
        return pd.DataFrame(orders)
    
    def generate_products_data(self) -> pd.DataFrame:
        """Generate product metadata"""
        products_data = []
        
        for product in self.products:
            # Generate random creation timestamp
            random_days = random.randint(-365, 0)  # 1 year before start date
            create_date = self.start_date + timedelta(days=random_days)
            timestamp_str = create_date.strftime("%Y/%m/%d %H:%M:%S")
            
            product_data = {
                'id': product['id'],
                'name': product['name'],
                'category': product['category'],
                'brand': product['brand'],
                'price': product['price'],
                'create_timestamp': timestamp_str,
                'description': f"High-quality {product['name']} from {product['brand']}"
            }
            
            products_data.append(product_data)
        
        return pd.DataFrame(products_data)
    
    def save_data(self, orders_df: pd.DataFrame, products_df: pd.DataFrame):
        """Save data to CSV files"""
        # Save order data
        orders_file = '../data/sales_2023_2025_realistic.csv'
        orders_df.to_csv(orders_file, index=False)
        print(f"Order data saved to: {orders_file}")

        
    def print_statistics(self, orders_df: pd.DataFrame):
        """Print data statistics"""
        print("\n" + "="*50)
        print("DATA GENERATION STATISTICS")
        print("="*50)
        print(f"Total Orders: {len(orders_df):,}")
        print(f"Date Range: {orders_df['create_timestamp'].min()} to {orders_df['create_timestamp'].max()}")
        print(f"Average Quantity: {orders_df['quantity'].mean():.2f}")
        print(f"Total Revenue: ${orders_df['subtotal'].sum():,.2f}")
        
        print(f"\nProduct Distribution:")
        product_stats = orders_df.groupby('product_id').agg({
            'quantity': 'sum',
            'subtotal': 'sum',
            'order_id': 'count'
        }).round(2)
        product_stats.columns = ['Total Quantity', 'Total Revenue', 'Order Count']
        print(product_stats)
        
        print(f"\nSeller Distribution:")
        seller_stats = orders_df.groupby('seller_id').agg({
            'quantity': 'sum',
            'subtotal': 'sum',
            'order_id': 'count'
        }).round(2)
        seller_stats.columns = ['Total Quantity', 'Total Revenue', 'Order Count']
        print(seller_stats)
        
        print(f"\nProduct-Seller Mapping Verification:")
        print("Each cell shows order count for [Product x Seller] combination:")
        product_seller_matrix = orders_df.groupby(['product_id', 'seller_id']).size().unstack(fill_value=0)
        print(product_seller_matrix)
        
        print(f"\nSeller Coverage Check:")
        for seller in self.sellers:
            if seller in product_seller_matrix.columns:
                product_count = (product_seller_matrix.loc[:, seller] > 0).sum()
                total_orders = seller_stats.loc[seller, 'Order Count'] if seller in seller_stats.index else 0
                print(f"  {seller}: sells {product_count} products, {total_orders} total orders")
            else:
                print(f"  {seller}: sells 0 products, 0 total orders")
        
        print("\n" + "="*50)
        print("✅ Data generation completed successfully!")
        print("📁 Files saved in data/ directory")
        print("📊 Ready for analysis and modeling")


def main():
    """Main function"""
    print("Sales Data Generator")
    print("This script generates realistic sales data for machine learning training")
    print("="*50)
    
    generator = SalesDataGenerator()
    
    # Generate data
    orders_df = generator.generate_orders()
    products_df = generator.generate_products_data()
    
    # Save data
    generator.save_data(orders_df, products_df)
    
    # Print statistics
    generator.print_statistics(orders_df)


if __name__ == "__main__":
    main() 