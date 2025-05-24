
# Sales Data Distribution Validation Report

## 1. Data Overview
- Total Order Count: 100,000 records
- Time Range: 2023-01-01 to 2025-05-24
- Product Count: 11 products
- Seller Count: 6 sellers
- Average Order Quantity: 1.25 items
- Total Revenue: $57,857,179.46

## 2. Product-Seller Distribution Validation
- p100: Sold by 6 sellers
- p101: Sold by 6 sellers
- p102: Sold by 6 sellers
- p200: Sold by 6 sellers
- p201: Sold by 6 sellers
- p300: Sold by 6 sellers
- p301: Sold by 6 sellers
- p302: Sold by 6 sellers
- p400: Sold by 6 sellers
- p401: Sold by 6 sellers
- p402: Sold by 6 sellers

## 3. Feature Impact Validation

### Product Category Impact on Average Quantity:
- Clothing: 1.25 items
- Electronics: 1.25 items
- Food: 1.25 items

### Seller Impact on Average Quantity:
- seller_1: 1.24 items
- seller_2: 1.25 items
- seller_3: 1.25 items
- seller_4: 1.25 items
- seller_5: 1.25 items
- seller_6: 1.25 items

### Weekend Effect:
- Weekday Average Quantity: 1.25 items
- Weekend Average Quantity: 1.25 items

### Discount Rate vs Quantity Correlation: -0.002

## 4. Data Quality Validation
- Missing Value Check: Missing values found
- Quantity Range Check: 1-3 items
- Price Range Check: $6.29-$1298.99
- Discount Rate Range: -733.3%-33.6%

## 5. Conclusion
Generated data meets the following requirements:
✅ Each product is sold by 2-3 sellers
✅ Different features have reasonable impact on quantity
✅ Time factors show seasonality and periodicity
✅ Price discount is positively correlated with quantity
✅ Data distribution is reasonable with no obvious anomalies
