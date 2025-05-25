import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SalesAnalyticsResponse, PredictionResponse } from '../models/interfaces';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private baseUrl = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  getSalesAnalytics(request: any): Observable<SalesAnalyticsResponse> {
    return this.http.post<SalesAnalyticsResponse>(`${this.baseUrl}/v1/sales/analytics`, request);
  }

  getSalesPrediction(request: any): Observable<PredictionResponse> {
    return this.http.post<PredictionResponse>(`${this.baseUrl}/v1/sales/predict`, request);
  }
} 