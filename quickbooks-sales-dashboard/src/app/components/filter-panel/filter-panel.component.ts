import { Component, Output, EventEmitter, OnInit } from '@angular/core';
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
export class FilterPanelComponent implements OnInit {
  @Output() filterChange = new EventEmitter<any>();

  filterForm: FormGroup;
  products = PRODUCTS;
  sellers = SELLERS;
  timeRanges = TIME_RANGES;
  categories = [...new Set(PRODUCTS.map(p => p.category))];
  topNOptions = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];

  constructor(private fb: FormBuilder) {
    this.filterForm = this.fb.group({
      sellerId: ['seller_1'],
      productId: ['p100'],
      category: [''],
      topN: [''],
      startDate: [new Date(2025, 4, 10)],
      endDate: [new Date(2025, 5, 10)],
      timeRange: ['']
    });

    this.filterForm.valueChanges.subscribe(value => {
      if (value.sellerId && value.startDate && value.endDate) {
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

  ngOnInit() {
    // 触发初始数据加载
    const initialValue = this.filterForm.value;
    if (initialValue.sellerId && initialValue.startDate && initialValue.endDate) {
      const formattedValue = {
        ...initialValue,
        startDate: this.formatDateForApi(initialValue.startDate),
        endDate: this.formatDateForApi(initialValue.endDate)
      };
      this.filterChange.emit(formattedValue);
    }
  }

  onProductIdChange() {
    // When Product ID is selected, clear Top N and Category
    this.filterForm.patchValue({
      topN: '',
      category: ''
    }, { emitEvent: false });
  }

  onCategoryChange() {
    // When Category is selected, clear Product ID
    this.filterForm.patchValue({
      productId: ''
    }, { emitEvent: false });
  }

  onTopNChange() {
    // When Top N is selected, clear Product ID
    this.filterForm.patchValue({
      productId: ''
    }, { emitEvent: false });
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