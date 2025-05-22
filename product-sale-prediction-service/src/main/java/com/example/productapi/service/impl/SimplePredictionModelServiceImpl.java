package com.example.productapi.service.impl;

import com.example.productapi.model.Order;
import com.example.productapi.repository.OrderRepository;
import com.example.productapi.service.MLModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 简化版的ML模型服务，不依赖JPMML，使用基于历史数据的统计方法进行预测
 * 适用于没有外部ML模型依赖的场景
 */
@Service
public class SimplePredictionModelServiceImpl implements MLModelService {

    private static final Logger logger = LoggerFactory.getLogger(SimplePredictionModelServiceImpl.class);
    private static final int DEFAULT_HISTORY_DAYS = 90; // 默认使用90天历史数据
    
    private boolean initialized = false;
    
    @Autowired
    private OrderRepository orderRepository;

    @Value("${ml.model.use.local.model:true}")
    private boolean useLocalModel;

    @PostConstruct
    @Override
    public void initializeModel() {
        // 这个简化版本不需要加载模型，只需检查数据库连接
        try {
            // 简单检查数据库连接
            long count = orderRepository.count();
            logger.info("Sales prediction service initialization succeeded, database has {} order records", count);
            initialized = true;
        } catch (Exception e) {
            logger.error("Sales prediction service initialization failed", e);
            initialized = false;
        }
    }

    @Override
    public boolean isModelInitialized() {
        return initialized;
    }

    @Override
    public Map<String, Object> predictFutureSales(String productId, String sellerId, Double unitPrice, Integer weeksAhead) {
        if (!initialized) {
            logger.error("Sales prediction service not initialized");
            throw new IllegalStateException("Sales prediction service not initialized");
        }
        
        if (weeksAhead == null || weeksAhead < 1) {
            weeksAhead = 4; // Default to predict 4 weeks
        }
        
        try {
            // Get historical sales data for this product
            List<Order> historicalOrders = getHistoricalOrders(productId, sellerId);
            
            if (historicalOrders.isEmpty()) {
                logger.warn("No historical order data found for product {} and seller {}, using mock prediction", productId, sellerId);
                return generateMockPrediction(productId, sellerId, unitPrice, weeksAhead);
            }
            
            // Calculate statistical metrics
            Map<DayOfWeek, List<Integer>> salesByDayOfWeek = analyzeWeekdayPatterns(historicalOrders);
            Map<Integer, List<Integer>> salesByDayOfMonth = analyzeDayOfMonthPatterns(historicalOrders);
            double averageDailySales = calculateAverageDailySales(historicalOrders);
            double recentTrend = calculateRecentTrend(historicalOrders);
            
            logger.info("Product {} seller {} average daily sales: {}, recent trend: {}",
                    productId, sellerId, averageDailySales, recentTrend > 1.0 ? "increasing" : "decreasing");
            
            // Prepare prediction results
            Map<String, Object> prediction = new HashMap<>();
            prediction.put("product_id", productId);
            prediction.put("seller_id", sellerId);
            prediction.put("unit_price", unitPrice);
            
            List<Map<String, Object>> predictions = new ArrayList<>();
            LocalDate currentDate = LocalDate.now();
            
            // Predict future sales for several weeks
            for (int week = 0; week < weeksAhead; week++) {
                double weeklyTotal = 0;
                
                // Predict daily sales for a week
                for (int day = 0; day < 7; day++) {
                    LocalDate predictionDate = currentDate.plusWeeks(week).plusDays(day);
                    double dailyPrediction = predictSingleDay(
                            predictionDate, 
                            salesByDayOfWeek, 
                            salesByDayOfMonth, 
                            averageDailySales,
                            recentTrend,
                            week + 1);
                    
                    weeklyTotal += dailyPrediction;
                }
                
                // Create weekly prediction
                Map<String, Object> weekPrediction = new HashMap<>();
                weekPrediction.put("week_number", week + 1);
                weekPrediction.put("prediction_date", currentDate.plusWeeks(week).format(DateTimeFormatter.ISO_LOCAL_DATE));
                weekPrediction.put("predicted_sales", Math.round(weeklyTotal));
                
                predictions.add(weekPrediction);
            }
            
            // Set historical data information
            LocalDateTime firstOrderTime = historicalOrders.get(0).getTimestamp();
            LocalDateTime lastOrderTime = historicalOrders.get(historicalOrders.size() - 1).getTimestamp();
            
            prediction.put("predictions", predictions);
            prediction.put("historical_data_used", Map.of(
                    "start_date", firstOrderTime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    "end_date", lastOrderTime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    "total_days", ChronoUnit.DAYS.between(firstOrderTime.toLocalDate(), lastOrderTime.toLocalDate()) + 1,
                    "order_count", historicalOrders.size()
            ));
            
            return prediction;
        } catch (Exception e) {
            logger.error("Sales prediction failed", e);
            return generateMockPrediction(productId, sellerId, unitPrice, weeksAhead);
        }
    }
    
    /**
     * Predict daily sales
     */
    private double predictSingleDay(LocalDate date, 
                                  Map<DayOfWeek, List<Integer>> salesByDayOfWeek,
                                  Map<Integer, List<Integer>> salesByDayOfMonth,
                                  double averageDailySales,
                                  double recentTrend,
                                  int weekAhead) {
        
        // Consider weekday factor (weekend usually has different sales)
        List<Integer> dayOfWeekSales = salesByDayOfWeek.getOrDefault(date.getDayOfWeek(), new ArrayList<>());
        double dayOfWeekFactor = 1.0;
        if (!dayOfWeekSales.isEmpty()) {
            double avgForThisDay = dayOfWeekSales.stream().mapToDouble(Integer::doubleValue).average().orElse(averageDailySales);
            dayOfWeekFactor = avgForThisDay / averageDailySales;
        }
        
        // Consider monthly same date factor (beginning or end of month may have special patterns)
        List<Integer> dayOfMonthSales = salesByDayOfMonth.getOrDefault(date.getDayOfMonth(), new ArrayList<>());
        double dayOfMonthFactor = 1.0;
        if (!dayOfMonthSales.isEmpty()) {
            double avgForThisMonthDay = dayOfMonthSales.stream().mapToDouble(Integer::doubleValue).average().orElse(averageDailySales);
            dayOfMonthFactor = avgForThisMonthDay / averageDailySales;
        }
        
        // Apply trend factor (predictive power decreases with time)
        double trendFactor = Math.pow(recentTrend, weekAhead * 0.5); // Trend impact decreases with time
        
        // Seasonal pattern can be added here
        
        // Combine various factors
        double prediction = averageDailySales * dayOfWeekFactor * dayOfMonthFactor * trendFactor;
        
        // Ensure prediction value is non-negative
        return Math.max(0, prediction);
    }
    
    /**
     * Analyze weekday sales patterns
     */
    private Map<DayOfWeek, List<Integer>> analyzeWeekdayPatterns(List<Order> orders) {
        Map<DayOfWeek, List<Integer>> salesByDayOfWeek = new EnumMap<>(DayOfWeek.class);
        
        // Group sales by weekday
        for (Order order : orders) {
            DayOfWeek dayOfWeek = order.getTimestamp().getDayOfWeek();
            salesByDayOfWeek.computeIfAbsent(dayOfWeek, k -> new ArrayList<>())
                           .add(order.getQuantity());
        }
        
        return salesByDayOfWeek;
    }
    
    /**
     * Analyze monthly date sales patterns
     */
    private Map<Integer, List<Integer>> analyzeDayOfMonthPatterns(List<Order> orders) {
        Map<Integer, List<Integer>> salesByDayOfMonth = new HashMap<>();
        
        // Group sales by date in month
        for (Order order : orders) {
            int dayOfMonth = order.getTimestamp().getDayOfMonth();
            salesByDayOfMonth.computeIfAbsent(dayOfMonth, k -> new ArrayList<>())
                           .add(order.getQuantity());
        }
        
        return salesByDayOfMonth;
    }
    
    /**
     * Calculate average daily sales
     */
    private double calculateAverageDailySales(List<Order> orders) {
        // Group sales by date, calculate total sales for each day
        Map<LocalDate, Integer> dailySales = new HashMap<>();
        
        for (Order order : orders) {
            LocalDate date = order.getTimestamp().toLocalDate();
            dailySales.merge(date, order.getQuantity(), Integer::sum);
        }
        
        // Calculate average value of all dates
        return dailySales.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
    }
    
    /**
     * Calculate recent sales trend (increasing/decreasing)
     */
    private double calculateRecentTrend(List<Order> orders) {
        if (orders.size() < 14) {
            return 1.0; // Insufficient data, return no change
        }
        
        // Sort orders by timestamp
        orders.sort(Comparator.comparing(Order::getTimestamp));
        
        // Group sales by date, calculate total sales for each day
        Map<LocalDate, Integer> dailySales = new HashMap<>();
        for (Order order : orders) {
            LocalDate date = order.getTimestamp().toLocalDate();
            dailySales.merge(date, order.getQuantity(), Integer::sum);
        }
        
        // Get unique dates and sort
        List<LocalDate> dates = new ArrayList<>(dailySales.keySet());
        dates.sort(Comparator.naturalOrder());
        
        if (dates.size() < 14) {
            return 1.0; // Insufficient days, return no change
        }
        
        // Calculate average daily sales for first half and second half
        int midPoint = dates.size() / 2;
        List<LocalDate> firstHalf = dates.subList(0, midPoint);
        List<LocalDate> secondHalf = dates.subList(midPoint, dates.size());
        
        double firstHalfAvg = firstHalf.stream()
                .mapToDouble(date -> dailySales.get(date))
                .average()
                .orElse(0.0);
        
        double secondHalfAvg = secondHalf.stream()
                .mapToDouble(date -> dailySales.get(date))
                .average()
                .orElse(0.0);
        
        // Avoid division by zero
        if (firstHalfAvg == 0) {
            return secondHalfAvg > 0 ? 1.5 : 1.0; // If first half is 0 but second half has sales, consider it increasing trend
        }
        
        // Calculate trend ratio
        return secondHalfAvg / firstHalfAvg;
    }
    
    /**
     * Get all historical order data
     */
    private List<Order> getHistoricalOrders(String productId, String sellerId) {
        // Use a very early date to ensure getting all historical data
        LocalDateTime longTimeAgo = LocalDateTime.now().minusYears(10); // 10 years ago
        List<Order> orders = orderRepository.findBySellerIdAndProductIdAndTimestampAfter(
                sellerId, productId, longTimeAgo);
        
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Sort by timestamp
        orders.sort(Comparator.comparing(Order::getTimestamp));
        return orders;
    }

    @Override
    public List<Map<String, Object>> getWeeklyPredictions(Map<String, Object> prediction) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> predictions = (List<Map<String, Object>>) prediction.get("predictions");
            return predictions != null ? predictions : new ArrayList<>();
        } catch (ClassCastException e) {
            logger.error("Invalid prediction format", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Generate mock prediction
     */
    private Map<String, Object> generateMockPrediction(String productId, String sellerId, Double unitPrice, Integer weeksAhead) {
        Map<String, Object> prediction = new HashMap<>();
        prediction.put("product_id", productId);
        prediction.put("seller_id", sellerId);
        prediction.put("unit_price", unitPrice);
        
        List<Map<String, Object>> predictions = new ArrayList<>();
        Random random = new Random();
        
        double baseQuantity = 50 + random.nextDouble() * 100;
        
        for (int week = 0; week < weeksAhead; week++) {
            Map<String, Object> weekPrediction = new HashMap<>();
            weekPrediction.put("week_number", week + 1);
            weekPrediction.put("prediction_date", LocalDate.now().plusWeeks(week).format(DateTimeFormatter.ISO_LOCAL_DATE));
            
            // Add some randomness but follow trend
            double weeklyFactor = 1.0 + (week * 0.05) + (random.nextDouble() * 0.1 - 0.05);
            double predictedSales = baseQuantity * weeklyFactor;
            
            weekPrediction.put("predicted_sales", Math.round(predictedSales));
            predictions.add(weekPrediction);
        }
        
        prediction.put("predictions", predictions);
        prediction.put("historical_data_used", Map.of(
                "start_date", "Mock data",
                "end_date", "Mock data",
                "total_days", 0,
                "order_count", 0
        ));
        
        return prediction;
    }
} 