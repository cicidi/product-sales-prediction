import { Component, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { PRODUCTS, SELLERS, TIME_RANGES } from '../../constants';

@Component({
  selector: 'app-filter-panel',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatDatepickerModule,
    MatNativeDateModule
  ],
  templateUrl: './filter-panel.component.html',
  styleUrls: ['./filter-panel.component.css']
})
export class FilterPanelComponent {
  @Output() filterChange = new EventEmitter<any>();

  filterForm: FormGroup;
  products = PRODUCTS;
  sellers = SELLERS;
  timeRanges = TIME_RANGES;

  constructor(private fb: FormBuilder) {
    this.filterForm = this.fb.group({
      sellerId: ['seller_1'],
      productId: [''],
      category: [''],
      startDate: [null],
      endDate: [null],
      timeRange: ['']
    });

    this.filterForm.valueChanges.subscribe(value => {
      if (value.sellerId && value.productId && value.startDate && value.endDate) {
        // 转换日期格式为API需要的格式
        const formattedValue = {
          ...value,
          startDate: this.formatDateForApi(value.startDate),
          endDate: this.formatDateForApi(value.endDate)
        };
        this.filterChange.emit(formattedValue);
      }
    });
  }

  onTimeRangeChange(range: string) {
    const today = new Date();
    let endDate = new Date();

    switch (range) {
      case 'week':
        endDate.setDate(today.getDate() + 7);
        break;
      case 'month':
        endDate.setMonth(today.getMonth() + 1);
        break;
      case 'year':
        endDate.setFullYear(today.getFullYear() + 1);
        break;
    }

    this.filterForm.patchValue({
      startDate: today,
      endDate: endDate
    });
  }

  private formatDateForApi(date: Date): string {
    if (!date) return '';
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
} 