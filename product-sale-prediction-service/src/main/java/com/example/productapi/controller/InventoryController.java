//package com.example.productapi.controller;
//
//import com.example.productapi.model.Inventory;
//import com.example.productapi.service.InventoryService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.Parameter;
//import io.swagger.v3.oas.annotations.media.Content;
//import io.swagger.v3.oas.annotations.media.Schema;
//import io.swagger.v3.oas.annotations.responses.ApiResponse;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/v1/inventory")
//@Tag(name = "Inventory", description = "Inventory management endpoints")
//public class InventoryController {
//
//    private final InventoryService inventoryService;
//
//    @Autowired
//    public InventoryController(InventoryService inventoryService) {
//        this.inventoryService = inventoryService;
//    }
//
//    @Operation(
//        summary = "Get inventory records",
//        description = "Get inventory records with optional filters for product ID and seller ID. " +
//                     "If both productId and sellerId are provided, returns specific inventory record. " +
//                     "If only sellerId is provided, returns all products for that seller. " +
//                     "If only productId is provided, returns all sellers for that product. " +
//                     "If no filters are provided, returns all inventory records."
//    )
//    @ApiResponse(
//        responseCode = "200",
//        description = "Successfully retrieved inventory records",
//        content = @Content(mediaType = "application/json", schema = @Schema(implementation = Inventory.class))
//    )
//    @GetMapping
//    public ResponseEntity<Map<String, Object>> getInventory(
//        @Parameter(description = "Product ID to filter by")
//        @RequestParam(required = false) String productId,
//
//        @Parameter(description = "Seller ID to filter by")
//        @RequestParam(required = false) String sellerId) {
//
//        List<Inventory> inventories = inventoryService.getInventoriesWithFilters(productId, sellerId);
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("inventories", inventories);
//        response.put("count", inventories.size());
//
//        // Add filter information to response
//        if (productId != null) {
//            response.put("productId", productId);
//        }
//        if (sellerId != null) {
//            response.put("sellerId", sellerId);
//        }
//
//        // Add description based on filters
//        String description;
//        if (productId != null && sellerId != null) {
//            description = "Specific inventory record for product " + productId + " and seller " + sellerId;
//        } else if (sellerId != null) {
//            description = "All products for seller " + sellerId;
//        } else if (productId != null) {
//            description = "All sellers for product " + productId;
//        } else {
//            description = "All inventory records";
//        }
//        response.put("description", description);
//
//        return ResponseEntity.ok(response);
//    }
//}