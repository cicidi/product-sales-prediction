package com.example.productapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PythonPredictionRequest {
    
    @JsonProperty("product_id")
    private String productId;
    
    @JsonProperty("seller_id")
    private String sellerId;
    
    @JsonProperty("sale_price")
    private Double salePrice;
    
    @JsonProperty("original_price")
    private Double originalPrice;
    
    @JsonProperty("is_holiday")
    private Integer isHoliday;
    
    @JsonProperty("is_weekend")
    private Integer isWeekend;
    
    @JsonProperty("day_of_week")
    private Integer dayOfWeek;
    
    @JsonProperty("day_of_month")
    private Integer dayOfMonth;
    
    @JsonProperty("month")
    private Integer month;
    
    @JsonProperty("lag_1")
    private Double lag1;
    
    @JsonProperty("lag_7")
    private Double lag7;
    
    @JsonProperty("lag_30")
    private Double lag30;
} 