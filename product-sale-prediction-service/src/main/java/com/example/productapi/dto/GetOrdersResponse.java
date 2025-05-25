package com.example.productapi.dto;

import com.example.productapi.model.Order;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response for order queries with pagination and aggregation data")
public class GetOrdersResponse {

    @Schema(description = "List of orders")
    private List<Order> orders;

    @Schema(description = "Query start time")
    private LocalDateTime startTime;

    @Schema(description = "Query end time")
    private LocalDateTime endTime;

    @Schema(description = "Current page number (0-based)")
    private Integer currentPage;

    @Schema(description = "Total number of items")
    private Long totalItems;

    @Schema(description = "Total number of pages")
    private Integer totalPages;

    @Schema(description = "Seller ID filter (if applied)")
    private String sellerId;

    @Schema(description = "Product ID filter (if applied)")
    private String productId;

    @Schema(description = "Category filter (if applied)")
    private String category;

    @Schema(description = "Daily product sales aggregation")
    private List<ProductSalesSummary> dailyProductSales;

    @Schema(description = "Total summary aggregation")
    private List<ProductSalesSummary> totalSummary;
} 