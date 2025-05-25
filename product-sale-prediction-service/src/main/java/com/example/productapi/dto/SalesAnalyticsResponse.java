package com.example.productapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Sales analytics response with daily and total summaries")
public class SalesAnalyticsResponse {

    @Schema(description = "Daily product sales summary")
    private List<ProductSalesSummary> dailyProductSales;

    @Schema(description = "Total product sales summary")
    private List<ProductSalesSummary> totalSummary;

    @Schema(description = "Query start time")
    private LocalDateTime startTime;

    @Schema(description = "Query end time")
    private LocalDateTime endTime;

    @Schema(description = "Seller ID filter (if applied)")
    private String sellerId;

    @Schema(description = "Product ID filter (if applied)")
    private String productId;

    @Schema(description = "Category filter (if applied)")
    private String category;

    @Schema(description = "Top N filter (only included when topN was requested)")
    private Integer topN;
} 