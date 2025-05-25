import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { SalesService } from '../../services/sales.service';
import { ChartData } from '../../models/interfaces';
import { FilterPanelComponent } from '../filter-panel/filter-panel.component';
import { DataTableComponent } from '../data-table/data-table.component';
import { SalesChartComponent } from '../sales-chart/sales-chart.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    FilterPanelComponent,
    DataTableComponent,
    SalesChartComponent
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent {
  salesData: ChartData[] = [];

  constructor(private salesService: SalesService) {}

  onFilterChange(filterData: any) {
    this.salesService.getSalesData(
      filterData.sellerId,
      filterData.productId,
      filterData.startDate,
      filterData.endDate
    ).subscribe(data => {
      this.salesData = data;
    });
  }
} 