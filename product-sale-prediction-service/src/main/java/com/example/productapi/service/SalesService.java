package com.example.productapi.service;

import com.example.productapi.model.Order;
import com.example.productapi.model.Product;
import com.example.productapi.repository.OrderRepository;
import com.example.productapi.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SalesService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final EmbeddingService embeddingService;
    
    @Autowired
    public SalesService(OrderRepository orderRepository, 
                      ProductRepository productRepository,
                      EmbeddingService embeddingService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.embeddingService = embeddingService;
    }
    
    /**
     * Get top selling products for a seller
     */
    public List<Map<String, Object>> getTopProductsBySeller(
            String sellerId, LocalDateTime startTime, LocalDateTime endTime, int topN) {
        
        List<Object[]> topResults = orderRepository.findTopProductsBySellerId(
            sellerId, startTime, endTime, PageRequest.of(0, topN));
            
        return convertTopProductsResults(topResults);
    }
    
    /**
     * Get top selling products for a seller, filtered by category
     */
    public List<Map<String, Object>> getTopProductsByCategoryAndSeller(
            String sellerId, String category, LocalDateTime startTime, LocalDateTime endTime, int topN) {
        
        List<Object[]> topResults = orderRepository.findTopProductsBySellerIdAndCategory(
            sellerId, category, startTime, endTime, PageRequest.of(0, topN));
            
        return convertTopProductsResults(topResults);
    }
    
    /**
     * Convert repository results to maps with product details
     */
    private List<Map<String, Object>> convertTopProductsResults(List<Object[]> results) {
        List<Map<String, Object>> mappedResults = new ArrayList<>();
        
        for (Object[] result : results) {
            String productId = (String) result[0];
            Long totalQuantity = (Long) result[1];
            
            Map<String, Object> productData = new HashMap<>();
            productData.put("productId", productId);
            productData.put("totalQuantity", totalQuantity);
            
            // Add product details if available
            productRepository.findById(productId).ifPresent(product -> {
                productData.put("name", product.getName());
                productData.put("category", product.getCategory());
                productData.put("brand", product.getBrand());
                productData.put("price", product.getPrice());
            });
            
            mappedResults.add(productData);
        }
        
        return mappedResults;
    }
    
    /**
     * Predict sales for a product using AWS SageMaker (mock implementation)
     */
    public Map<String, Object> predictProductSales(Product product, String sellerId, 
                                                 LocalDateTime startTime, LocalDateTime endTime) {
        // In a real implementation, we would:
        // 1. Get historical sales data for this product and seller
        // 2. Generate embeddings for the product description
        // 3. Prepare request payload for SageMaker endpoint
        // 4. Call SageMaker and parse results
        
        // For now, we'll return mock prediction data
        Map<String, Object> prediction = new HashMap<>();
        
        // Get historical data (this would be used in the SageMaker request)
        List<Order> historicalOrders = orderRepository.findBySellerIdAndProductIdAndTimestampBetween(
            sellerId, product.getId(), startTime, endTime);
        
        int totalHistoricalSales = historicalOrders.stream()
            .mapToInt(Order::getQuantity)
            .sum();
        
        // Mock prediction data
        prediction.put("predictedSalesQuantity", (int)(totalHistoricalSales * 1.2)); // 20% growth
        prediction.put("confidenceScore", 0.85);
        prediction.put("trendDirection", "up");
        prediction.put("historicalSalesCount", totalHistoricalSales);
        
        // Add monthly forecast (mock data)
        LocalDateTime forecastStart = endTime.plusDays(1);
        List<Map<String, Object>> monthlyForecast = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Map<String, Object> month = new HashMap<>();
            LocalDateTime monthDate = forecastStart.plusMonths(i);
            month.put("month", monthDate.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            month.put("predictedQuantity", (int)(totalHistoricalSales * (1.1 + (i * 0.1))));
            monthlyForecast.add(month);
        }
        prediction.put("monthlyForecast", monthlyForecast);
        
        return prediction;
    }
} 