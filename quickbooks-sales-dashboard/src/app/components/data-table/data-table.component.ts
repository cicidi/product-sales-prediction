import { Component, Input, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatCardModule } from '@angular/material/card';
import { ChartData } from '../../models/interfaces';

@Component({
  selector: 'app-data-table',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatPaginatorModule,
    MatCardModule
  ],
  templateUrl: './data-table.component.html',
  styleUrls: ['./data-table.component.css']
})
export class DataTableComponent {
  @Input() set data(value: ChartData[]) {
    const sortedData = (value || []).sort((a, b) => 
      new Date(b.date).getTime() - new Date(a.date).getTime()
    );
    this.dataSource.data = sortedData;
  }

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  displayedColumns: string[] = ['date', 'quantity', 'type'];
  dataSource = new MatTableDataSource<ChartData>();

  ngAfterViewInit() {
    this.dataSource.paginator = this.paginator;
  }
} 