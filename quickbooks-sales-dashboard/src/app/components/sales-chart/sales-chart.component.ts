import { Component, Input, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { Chart, ChartConfiguration, ChartType } from 'chart.js/auto';
import { ChartData } from '../../models/interfaces';

@Component({
  selector: 'app-sales-chart',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule
  ],
  templateUrl: './sales-chart.component.html',
  styleUrls: ['./sales-chart.component.css']
})
export class SalesChartComponent {
  @ViewChild('chartCanvas', { static: true }) chartCanvas!: ElementRef<HTMLCanvasElement>;
  
  private chart?: Chart;

  @Input() set data(value: ChartData[]) {
    if (value && value.length > 0) {
      this.updateChart(value);
    }
  }

  ngOnInit() {
    // 只在浏览器环境中初始化图表
    if (typeof window !== 'undefined') {
      this.initChart();
    }
  }

  ngOnDestroy() {
    if (this.chart) {
      this.chart.destroy();
    }
  }

  private initChart() {
    const ctx = this.chartCanvas.nativeElement.getContext('2d');
    if (!ctx) return;

    const config: ChartConfiguration = {
      type: 'line' as ChartType,
      data: {
        labels: [],
        datasets: [
          {
            label: 'Historical Sales',
            data: [],
            borderColor: '#2196F3',
            backgroundColor: 'rgba(33, 150, 243, 0.1)',
            tension: 0.1
          },
          {
            label: 'Predicted Sales',
            data: [],
            borderColor: '#F44336',
            backgroundColor: 'rgba(244, 67, 54, 0.1)',
            tension: 0.1
          }
        ]
      },
      options: {
        responsive: true,
        scales: {
          y: {
            beginAtZero: true,
            title: {
              display: true,
              text: 'Quantity'
            }
          },
          x: {
            title: {
              display: true,
              text: 'Date'
            }
          }
        }
      }
    };

    this.chart = new Chart(ctx, config);
  }

  private updateChart(data: ChartData[]) {
    if (!this.chart) return;

    const historical = data.filter(d => d.type === 'historical');
    const prediction = data.filter(d => d.type === 'prediction');

    const labels = [...new Set(data.map(d => d.date))];
    this.chart.data.labels = labels;
    this.chart.data.datasets[0].data = labels.map(date => {
      const item = historical.find(d => d.date === date);
      return item ? item.quantity : null;
    });
    this.chart.data.datasets[1].data = labels.map(date => {
      const item = prediction.find(d => d.date === date);
      return item ? item.quantity : null;
    });

    this.chart.update();
  }
} 