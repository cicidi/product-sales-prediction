import { Injectable } from '@angular/core';
import { Observable, forkJoin } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { ApiService } from './api.service';
import { ChartData } from '../models/interfaces';

@Injectable({
  providedIn: 'root'
})
export class SalesService {
  constructor(private apiService: ApiService) {}

  getSalesData(sellerId: string, productId: string, startDate: string, endDate: string, topN?: number, category?: string): Observable<ChartData[]> {
    const analyticsRequest = {
      topN: topN || 100,
      startTime: startDate,
      endTime: '2025-05-24',
      sellerId,
      ...(topN ? {} : { productId }),
      ...(category && { category })
    };

    const predictionRequest = {
      productId,
      sellerId,
      startDate: '2025-05-25',
      endDate
    };

    if (topN) {
      // For topN, get analytics first, then get predictions for each product
      return this.apiService.getSalesAnalytics(analyticsRequest).pipe(
        switchMap(analytics => {
          const historicalData: ChartData[] = analytics.dailyProductSales.map(item => ({
            date: item.date,
            quantity: item.quantity,
            type: 'historical' as const,
            productId: item.productId
          }));

          // Get unique product IDs from analytics result
          const productIds = [...new Set(analytics.dailyProductSales.map(item => item.productId))];
          
          // Create prediction requests for each product
          const predictionRequests = productIds.map(pid => 
            this.apiService.getSalesPrediction({
              productId: pid,
              sellerId,
              startDate: '2025-05-25',
              endDate
            })
          );

          return forkJoin(predictionRequests).pipe(
            map(predictions => {
              const allPredictionData: ChartData[] = [];
              predictions.forEach((prediction, index) => {
                const predictionData = prediction.predicationList.map(item => ({
                  date: `${item.date[0]}/${String(item.date[1]).padStart(2, '0')}/${String(item.date[2]).padStart(2, '0')}`,
                  quantity: item.quantity,
                  type: 'prediction' as const,
                  productId: productIds[index]
                }));
                allPredictionData.push(...predictionData);
              });

              return [...historicalData, ...allPredictionData].sort((a, b) => 
                new Date(a.date).getTime() - new Date(b.date).getTime()
              );
            })
          );
        })
      );
    } else {
      // Single product flow
      return forkJoin({
        analytics: this.apiService.getSalesAnalytics(analyticsRequest),
        prediction: this.apiService.getSalesPrediction(predictionRequest)
      }).pipe(
        map(({ analytics, prediction }) => {
          const historicalData: ChartData[] = analytics.dailyProductSales.map(item => ({
            date: item.date,
            quantity: item.quantity,
            type: 'historical' as const,
            productId: item.productId
          }));

          const predictionData: ChartData[] = prediction.predicationList.map(item => ({
            date: `${item.date[0]}/${String(item.date[1]).padStart(2, '0')}/${String(item.date[2]).padStart(2, '0')}`,
            quantity: item.quantity,
            type: 'prediction' as const,
            productId
          }));

          return [...historicalData, ...predictionData].sort((a, b) => 
            new Date(a.date).getTime() - new Date(b.date).getTime()
          );
        })
      );
    }
  }

  formatDateForApi(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
} 