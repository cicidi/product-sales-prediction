#!/usr/bin/env python3

import csv
import random
import uuid
from datetime import datetime, timedelta

# Path to the CSV file
csv_file_path = 'src/main/resources/sales_2023_2025_realistic.csv'

# Read existing records
with open(csv_file_path, 'r') as file:
    reader = csv.reader(file)
    header = next(reader)  # Read header row
    records = list(reader)

# Generate 10 random sales records for p403 with seller_6
new_records = []
product_id = 'p403'
seller_id = 'seller_6'
buyer_ids = [f"user_{random.randint(1000, 9999)}" for _ in range(10)]
unit_prices = [random.uniform(800, 1200) for _ in range(10)]

# Generate dates between 2023-01-01 and 2025-12-31
start_date = datetime(2023, 1, 1)
end_date = datetime(2025, 12, 31)
dates = []
for _ in range(10):
    delta = random.randint(0, (end_date - start_date).days)
    date = start_date + timedelta(days=delta)
    dates.append(date.strftime("%Y-%m-%dT00:00:00"))

for i in range(10):
    order_id = str(uuid.uuid4())
    buyer_id = buyer_ids[i]
    unit_price = round(unit_prices[i], 2)
    quantity = random.randint(1, 5)
    total_price = round(unit_price * quantity, 2)
    timestamp = dates[i]
    
    new_record = [
        order_id,
        product_id,
        buyer_id,
        seller_id,
        str(unit_price),
        str(quantity),
        str(total_price),
        timestamp
    ]
    new_records.append(new_record)

# Add new records to the CSV file
all_records = records + new_records
with open(csv_file_path, 'w', newline='') as file:
    writer = csv.writer(file)
    writer.writerow(header)
    writer.writerows(all_records)

print(f"Added {len(new_records)} sales records for {product_id} with {seller_id} to {csv_file_path}") 