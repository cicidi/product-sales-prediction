package com.example.productapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PredictionRequest {
    
    @Schema(description = "Product ID", example = "P123456", required = true)
    private String productId;
    
    @Schema(description = "Seller ID", example = "seller_1", required = true)
    private String sellerId;
    
    @Schema(description = "Sale price (optional, if not provided will use original price)", example = "99.99")
    private Double salePrice;
    
    @Schema(description = "Start date for prediction", example = "2025/05/01", required = true)
    private LocalDate startDate;
    
    @Schema(description = "End date for prediction (optional, if not provided will only predict one day)", example = "2025/05/31")
    private LocalDate endDate;
} 