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
    
    @Schema(description = "Start time for search (format: yyyy/MM)", example = "2023/05", required = true)
    private String startTime;
    
    @Schema(description = "End time for search (format: yyyy/MM), defaults to current time", example = "2024/05")
    private String endTime;
    
    @Schema(description = "Product category to filter results", example = "electronics")
    private String category;
    
    @Schema(description = "Seller ID to filter sales", example = "seller123", required = true)
    private String sellerId;
} 