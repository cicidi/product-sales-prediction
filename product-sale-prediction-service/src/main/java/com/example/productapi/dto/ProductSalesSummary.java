package com.example.productapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Product sales summary with productId, quantity, date, and optional revenue")
public class ProductSalesSummary {

    @Schema(description = "Product ID", example = "p100")
    private String productId;

    @Schema(description = "Total quantity sold", example = "150")
    private Integer quantity;

    @Schema(description = "Date in yyyy/MM/dd format, or 'total' for summary", example = "2024/01/01")
    private String date;

    @Schema(description = "Total revenue (optional)", example = "1500.00")
    private Double totalRevenue;
} 