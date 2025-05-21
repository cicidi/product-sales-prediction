package com.example.productapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesPredictionRequest {
    
    @Schema(description = "Product ID to predict sales for", example = "10", required = true)
    private String productId;
    
    @Schema(description = "Start time for historical data (format: yyyy/MM)", example = "2023/05", required = true)
    private String startTime;
    
    @Schema(description = "End time for prediction (format: yyyy/MM)", example = "2024/05")
    private String endTime;
    
    @Schema(description = "Product category filter", example = "electronics")
    private String category;
    
    @Schema(description = "Seller ID", example = "seller123", required = true)
    private String sellerId;
} 