package com.example.productapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimilarProductSearchRequest {
    
    @Schema(description = "Product ID to find similar products", example = "abc123")
    private String productId;
    
    @Schema(description = "Description text to find similar products", example = "high-end laptop")
    private String description;
} 