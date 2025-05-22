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
    
    @Schema(description = "Product ID", example = "p100", required = true)
    private String productId;
    
    @Schema(description = "Seller ID", example = "seller_1", required = true)
    private String sellerId;
    
    @Schema(description = "Unit price", example = "99.99", required = true)
    private Double unitPrice;
    
    @Schema(description = "Number of weeks to predict into the future", example = "4", required = false)
    private Integer numberOfWeekInFuture = 4;
} 