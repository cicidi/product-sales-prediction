import { Product } from './models/interfaces';

export const PRODUCTS: Product[] = [
  {
    id: 'p100',
    name: 'Apple iPhone 14 128GB',
    category: 'electronics',
    brand: 'Apple',
    price: 799.0,
    createTimestamp: '2023-04-30T00:00:00',
    description: 'iPhone 14 with A15 chip.'
  },
  {
    id: 'p101',
    name: 'Apple iPhone 15 Pro 256GB',
    category: 'electronics',
    brand: 'Apple',
    price: 1099.0,
    createTimestamp: '2024-04-30T00:00:00',
    description: 'iPhone 15 Pro with A17 chip.'
  },
  {
    id: 'p102',
    name: 'Apple iPhone 16 Pro Max 512GB',
    category: 'electronics',
    brand: 'Apple',
    price: 1299.0,
    createTimestamp: '2025-04-30T00:00:00',
    description: 'iPhone 16 Pro Max with improved camera and display.'
  },
  {
    id: 'p200',
    name: 'Samsung Galaxy S24 Ultra',
    category: 'electronics',
    brand: 'Samsung',
    price: 1099.99,
    createTimestamp: '2024-02-10T00:00:00',
    description: 'High-end Android phone with advanced camera.'
  },
  {
    id: 'p201',
    name: 'Google Pixel 8 Pro',
    category: 'electronics',
    brand: 'Google',
    price: 999.99,
    createTimestamp: '2024-01-15T00:00:00',
    description: 'Pixel phone with best-in-class AI features.'
  },
  {
    id: 'p300',
    name: 'Nike Air Zoom Pegasus 40',
    category: 'clothes',
    brand: 'Nike',
    price: 130.0,
    createTimestamp: '2024-03-01T00:00:00',
    description: 'Responsive running shoes for everyday training.'
  },
  {
    id: 'p301',
    name: 'Uniqlo Ultra Light Down Jacket',
    category: 'clothes',
    brand: 'Uniqlo',
    price: 79.9,
    createTimestamp: '2023-11-01T00:00:00',
    description: 'Compact and warm down jacket.'
  },
  {
    id: 'p302',
    name: 'Adidas 3-Stripes T-Shirt',
    category: 'clothes',
    brand: 'Adidas',
    price: 25.0,
    createTimestamp: '2023-06-15T00:00:00',
    description: 'Classic t-shirt with Adidas stripes.'
  },
  {
    id: 'p400',
    name: 'Kirkland Organic Almond Butter',
    category: 'food',
    brand: 'Kirkland',
    price: 12.99,
    createTimestamp: '2023-09-05T00:00:00',
    description: 'Smooth and creamy organic almond butter.'
  },
  {
    id: 'p401',
    name: 'Blue Diamond Whole Natural Almonds',
    category: 'food',
    brand: 'Blue Diamond',
    price: 10.49,
    createTimestamp: '2023-07-12T00:00:00',
    description: 'Healthy roasted almonds for snacking.'
  },
  {
    id: 'p402',
    name: 'Lindt Swiss Chocolate Assorted',
    category: 'food',
    brand: 'Lindt',
    price: 15.99,
    createTimestamp: '2023-12-20T00:00:00',
    description: 'Premium assorted Swiss chocolate truffles.'
  }
];

export const SELLERS = ['seller_1', 'seller_2', 'seller_3', 'seller_4', 'seller_5', 'seller_6'];

export const CATEGORIES = ['electronics', 'clothes', 'food'];

export const TIME_RANGES = [
  { label: 'Next Week', value: 'week' },
  { label: 'Next Month', value: 'month' },
  { label: 'Next Year', value: 'year' }
]; 