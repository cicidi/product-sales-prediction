package com.example.productapi.controller;

import com.example.productapi.mcp.model.ToolResponse;
import com.example.productapi.model.Order;
import com.example.productapi.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1")
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {

  private final OrderService orderService;

  @Autowired
  public OrderController(OrderService orderService) {
    this.orderService = orderService;
  }

  @Operation(
      summary = "Get orders with filters (paginated)",
      description = "Retrieve orders with optional filters for seller ID, product ID, category, date range. Results are sorted by timestamp in descending order. Default returns last 30 days orders."
  )
  @ApiResponse(
      responseCode = "200",
      description = "Successfully retrieved orders",
      content = @Content(mediaType = "application/json", schema = @Schema(implementation = Order.class))
  )
  @GetMapping("/orders")
  public ResponseEntity<Map<String, Object>> getOrders(
      @Parameter(description = "Seller ID to filter orders by")
      @RequestParam(required = false) String sellerId,

      @Parameter(description = "Product ID to filter orders by")
      @RequestParam(required = false) String productId,

      @Parameter(description = "Category to filter orders by")
      @RequestParam(required = false) String category,

      @Parameter(description = "Start time (ISO format, e.g. 2024-03-21T00:00:00)")
      @RequestParam(required = false) String startTime,

      @Parameter(description = "End time (ISO format, e.g. 2024-03-21T23:59:59). Defaults to current time if not provided")
      @RequestParam(required = false) String endTime,

      @Parameter(description = "Page number (0-based)")
      @RequestParam(defaultValue = "0") int page,

      @Parameter(description = "Page size (max 100)")
      @RequestParam(defaultValue = "20") int size) {

    // Enforce max size of 100
    if (size > 100) {
      size = 100;
    }

    // Parse end time, default to now if not provided
    LocalDateTime endDateTime = endTime != null ?
        LocalDateTime.parse(endTime) :
        LocalDateTime.now();

    // Parse start time, default to 30 days ago if not provided
    LocalDateTime startDateTime = startTime != null ?
        LocalDateTime.parse(startTime) :
        endDateTime.minus(30, ChronoUnit.DAYS);

    // Get orders with filters
    Page<Order> orderPage = orderService.getOrdersWithFilters(
        sellerId,
        productId,
        category,
        startDateTime,
        endDateTime,
        page,
        size
    );

    Map<String, Object> response = new HashMap<>();
    response.put("orders", orderPage.getContent());
    response.put("currentPage", orderPage.getNumber());
    response.put("totalItems", orderPage.getTotalElements());
    response.put("totalPages", orderPage.getTotalPages());

    // Add filter information to response
    if (sellerId != null) {
      response.put("sellerId", sellerId);
    }
    if (productId != null) {
      response.put("productId", productId);
    }
    if (category != null) {
      response.put("category", category);
    }
    response.put("startTime", startDateTime.toString());
    response.put("endTime", endDateTime.toString());

    return ResponseEntity.ok(response);
  }
}