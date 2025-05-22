package com.example.productapi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesPredictionRequest {
    
    @Schema(description = "产品ID", example = "p100", required = true)
    private String productId;
    
    @Schema(description = "卖家ID", example = "seller_1", required = true)
    private String sellerId;
    
    @Schema(description = "单价", example = "99.99", required = true)
    private Double unitPrice;
    
    @Schema(description = "预测未来几周的销量", example = "4", required = false)
    private Integer numberOfWeekInFuture = 4;
} 