package com.example.productapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopSellingProductResponse {
    
    @Schema(description = "Product ID", example = "P123456")
    private String productId;
    
    @Schema(description = "Product name", example = "Wireless Headphones")
    private String name;
    
    @Schema(description = "Product category", example = "Electronics")
    private String category;
    
    @Schema(description = "Product brand", example = "Sony")
    private String brand;
    
    @Schema(description = "Current unit price", example = "99.99")
    private Double price;
    
    @Schema(description = "Total quantity sold", example = "150")
    private Long totalQuantity;
    
    @Schema(description = "Total revenue generated", example = "14998.50")
    private Double totalRevenue;
} 