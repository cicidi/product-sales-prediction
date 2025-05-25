package com.example.productapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesSearchRequest {
    
    @Schema(description = "Number of top selling products to return", example = "10", required = true)
    private Integer topN;
    
    @Schema(description = "Start time for search (format: yyyy-MM-dd)", example = "2025-05-01", required = true)
    private String startTime;
    
    @Schema(description = "End time for search (format: yyyy-MM-dd), defaults to current time", example = "2025-05-01")
    private String endTime;
    
    @Schema(description = "Product category to filter results", example = "electronics")
    private String category;
    
    @Schema(description = "Seller ID to filter sales (optional)", example = "seller_1")
    private String sellerId;
    
    @Schema(description = "Product ID to filter sales (optional)", example = "p100")
    private String productId;
} 