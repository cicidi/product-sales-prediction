export interface Product {
  id: string;
  name: string;
  category: string;
  brand: string;
  price: number;
  createTimestamp: string;
  description: string;
}

export interface SalesData {
  productId: string;
  quantity: number;
  date: string;
  totalRevenue: number;
}

export interface PredictionData {
  date: number[];
  quantity: number;
}

export interface SalesAnalyticsResponse {
  dailyProductSales: SalesData[];
  totalSummary: SalesData[];
  startTime: number[];
  endTime: number[];
  sellerId: string;
  productId: string;
  category: string | null;
  topN: number;
}

export interface PredictionResponse {
  predicationList: PredictionData[];
  startDate: number[];
  endDate: number[];
  totalQuantity: number;
  totalDays: number;
}

export interface ChartData {
  date: string;
  quantity: number;
  type: 'historical' | 'prediction';
  productId?: string;
} 