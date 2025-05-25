package com.example.productapi.controller;

import com.example.productapi.model.Product;
import com.example.productapi.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1")
@Tag(name = "Products", description = "Product management endpoints")
public class ProductController {

  private final ProductService productService;

  @Autowired
  public ProductController(ProductService productService) {
    this.productService = productService;
  }

  @Operation(
      summary = "Get all products",
      description = "Retrieve all products, optionally filtered by category and/or seller ID"
  )
  @ApiResponse(
      responseCode = "200",
      description = "Successfully retrieved products",
      content = @Content(mediaType = "application/json", schema = @Schema(implementation = Product.class))
  )
  @GetMapping("/products")
  public ResponseEntity<Map<String, Object>> getAllProducts(
      @Parameter(description = "Filter by product category")
      @RequestParam(required = false) String category,

      @Parameter(description = "Filter by seller ID")
      @RequestParam(required = false) String sellerId) {

    List<Product> products = productService.getAllProducts(category, sellerId);

    Map<String, Object> response = new HashMap<>();
    response.put("products", products);
    response.put("count", products.size());

    if (category != null && !category.isEmpty()) {
      response.put("category", category);
    }

    if (sellerId != null && !sellerId.isEmpty()) {
      response.put("sellerId", sellerId);
    }

    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Get product by ID",
      description = "Retrieve a product by its unique identifier"
  )
  @ApiResponse(
      responseCode = "200",
      description = "Product found",
      content = @Content(mediaType = "application/json", schema = @Schema(implementation = Product.class))
  )
  @ApiResponse(
      responseCode = "404",
      description = "Product not found"
  )
  @GetMapping("/product/{id}")
  public ResponseEntity<Map<String, Object>> getProductById(
      @Parameter(description = "Product ID", required = true)
      @PathVariable String id) {

    Optional<Product> optProduct = productService.getProductById(id);

    if (optProduct.isPresent()) {
      Map<String, Object> response = new HashMap<>();
      response.put("product", optProduct.get());
      return ResponseEntity.ok(response);
    } else {
      Map<String, Object> response = new HashMap<>();
      response.put("error", "Product not found");
      response.put("id", id);
      return ResponseEntity.notFound().build();
    }
  }
}