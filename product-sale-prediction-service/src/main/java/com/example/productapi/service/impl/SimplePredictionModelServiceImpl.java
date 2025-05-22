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
            logger.info("销售预测服务初始化成功，数据库中有 {} 条订单记录", count);
            initialized = true;
        } catch (Exception e) {
            logger.error("销售预测服务初始化失败", e);
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
            logger.error("销售预测服务未初始化");
            throw new IllegalStateException("销售预测服务未初始化");
        }
        
        if (weeksAhead == null || weeksAhead < 1) {
            weeksAhead = 4; // 默认预测4周
        }
        
        try {
            // 获取该产品的历史销售数据
            List<Order> historicalOrders = getHistoricalOrders(productId, sellerId);
            
            if (historicalOrders.isEmpty()) {
                logger.warn("没有找到产品 {} 的卖家 {} 的历史订单数据，使用模拟预测", productId, sellerId);
                return generateMockPrediction(productId, sellerId, unitPrice, weeksAhead);
            }
            
            // 计算统计指标
            Map<DayOfWeek, List<Integer>> salesByDayOfWeek = analyzeWeekdayPatterns(historicalOrders);
            Map<Integer, List<Integer>> salesByDayOfMonth = analyzeDayOfMonthPatterns(historicalOrders);
            double averageDailySales = calculateAverageDailySales(historicalOrders);
            double recentTrend = calculateRecentTrend(historicalOrders);
            
            logger.info("产品 {} 卖家 {} 的平均日销量: {}, 近期趋势: {}",
                    productId, sellerId, averageDailySales, recentTrend > 1.0 ? "上升" : "下降");
            
            // 准备预测结果
            Map<String, Object> prediction = new HashMap<>();
            prediction.put("product_id", productId);
            prediction.put("seller_id", sellerId);
            prediction.put("unit_price", unitPrice);
            
            List<Map<String, Object>> predictions = new ArrayList<>();
            LocalDate currentDate = LocalDate.now();
            
            // 预测未来几周的销售情况
            for (int week = 0; week < weeksAhead; week++) {
                double weeklyTotal = 0;
                
                // 按天预测一周销量
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
                
                // 创建每周预测
                Map<String, Object> weekPrediction = new HashMap<>();
                weekPrediction.put("week_number", week + 1);
                weekPrediction.put("prediction_date", currentDate.plusWeeks(week).format(DateTimeFormatter.ISO_LOCAL_DATE));
                weekPrediction.put("predicted_sales", Math.round(weeklyTotal));
                
                predictions.add(weekPrediction);
            }
            
            // 设置历史数据信息
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
            logger.error("销售预测失败", e);
            return generateMockPrediction(productId, sellerId, unitPrice, weeksAhead);
        }
    }
    
    /**
     * 预测单天的销量
     */
    private double predictSingleDay(LocalDate date, 
                                  Map<DayOfWeek, List<Integer>> salesByDayOfWeek,
                                  Map<Integer, List<Integer>> salesByDayOfMonth,
                                  double averageDailySales,
                                  double recentTrend,
                                  int weekAhead) {
        
        // 考虑星期几因素（周末通常销量不同）
        List<Integer> dayOfWeekSales = salesByDayOfWeek.getOrDefault(date.getDayOfWeek(), new ArrayList<>());
        double dayOfWeekFactor = 1.0;
        if (!dayOfWeekSales.isEmpty()) {
            double avgForThisDay = dayOfWeekSales.stream().mapToDouble(Integer::doubleValue).average().orElse(averageDailySales);
            dayOfWeekFactor = avgForThisDay / averageDailySales;
        }
        
        // 考虑每月相同日期的因素（月初或月末可能有特殊模式）
        List<Integer> dayOfMonthSales = salesByDayOfMonth.getOrDefault(date.getDayOfMonth(), new ArrayList<>());
        double dayOfMonthFactor = 1.0;
        if (!dayOfMonthSales.isEmpty()) {
            double avgForThisMonthDay = dayOfMonthSales.stream().mapToDouble(Integer::doubleValue).average().orElse(averageDailySales);
            dayOfMonthFactor = avgForThisMonthDay / averageDailySales;
        }
        
        // 应用趋势因素（预测越远，趋势影响越大）
        double trendFactor = Math.pow(recentTrend, weekAhead * 0.5); // 随时间平滑衰减趋势影响
        
        // 季节模式可以在此添加
        
        // 综合各种因素
        double prediction = averageDailySales * dayOfWeekFactor * dayOfMonthFactor * trendFactor;
        
        // 确保预测值非负
        return Math.max(0, prediction);
    }
    
    /**
     * 分析星期几的销售模式
     */
    private Map<DayOfWeek, List<Integer>> analyzeWeekdayPatterns(List<Order> orders) {
        Map<DayOfWeek, List<Integer>> salesByDayOfWeek = new EnumMap<>(DayOfWeek.class);
        
        // 按星期几分组销量
        for (Order order : orders) {
            DayOfWeek dayOfWeek = order.getTimestamp().getDayOfWeek();
            salesByDayOfWeek.computeIfAbsent(dayOfWeek, k -> new ArrayList<>())
                           .add(order.getQuantity());
        }
        
        return salesByDayOfWeek;
    }
    
    /**
     * 分析月份日期的销售模式
     */
    private Map<Integer, List<Integer>> analyzeDayOfMonthPatterns(List<Order> orders) {
        Map<Integer, List<Integer>> salesByDayOfMonth = new HashMap<>();
        
        // 按月份中的日期分组销量
        for (Order order : orders) {
            int dayOfMonth = order.getTimestamp().getDayOfMonth();
            salesByDayOfMonth.computeIfAbsent(dayOfMonth, k -> new ArrayList<>())
                           .add(order.getQuantity());
        }
        
        return salesByDayOfMonth;
    }
    
    /**
     * 计算平均日销量
     */
    private double calculateAverageDailySales(List<Order> orders) {
        // 按日期分组，计算每天的总销量
        Map<LocalDate, Integer> dailySales = new HashMap<>();
        
        for (Order order : orders) {
            LocalDate date = order.getTimestamp().toLocalDate();
            dailySales.merge(date, order.getQuantity(), Integer::sum);
        }
        
        // 计算所有日期的平均值
        return dailySales.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
    }
    
    /**
     * 计算最近的销售趋势（上升/下降）
     */
    private double calculateRecentTrend(List<Order> orders) {
        if (orders.size() < 14) {
            return 1.0; // 数据不足，返回无变化
        }
        
        // 对订单按时间戳排序
        orders.sort(Comparator.comparing(Order::getTimestamp));
        
        // 按日期分组，计算每天的总销量
        Map<LocalDate, Integer> dailySales = new HashMap<>();
        for (Order order : orders) {
            LocalDate date = order.getTimestamp().toLocalDate();
            dailySales.merge(date, order.getQuantity(), Integer::sum);
        }
        
        // 获取唯一日期并排序
        List<LocalDate> dates = new ArrayList<>(dailySales.keySet());
        dates.sort(Comparator.naturalOrder());
        
        if (dates.size() < 14) {
            return 1.0; // 天数不足，返回无变化
        }
        
        // 计算前半部分和后半部分的平均日销量
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
        
        // 避免除以零
        if (firstHalfAvg == 0) {
            return secondHalfAvg > 0 ? 1.5 : 1.0; // 如果前半部分为0但后半部分有销量，则认为是上升趋势
        }
        
        // 计算趋势比例
        return secondHalfAvg / firstHalfAvg;
    }
    
    /**
     * 获取所有历史订单数据
     */
    private List<Order> getHistoricalOrders(String productId, String sellerId) {
        // 使用一个很早的日期确保获取全部历史数据
        LocalDateTime longTimeAgo = LocalDateTime.now().minusYears(10); // 10年前
        List<Order> orders = orderRepository.findBySellerIdAndProductIdAndTimestampAfter(
                sellerId, productId, longTimeAgo);
        
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 按时间戳排序
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
            logger.error("预测格式无效", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 生成模拟预测
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
            
            // 添加一些随机性但遵循趋势
            double weeklyFactor = 1.0 + (week * 0.05) + (random.nextDouble() * 0.1 - 0.05);
            double predictedSales = baseQuantity * weeklyFactor;
            
            weekPrediction.put("predicted_sales", Math.round(predictedSales));
            predictions.add(weekPrediction);
        }
        
        prediction.put("predictions", predictions);
        prediction.put("historical_data_used", Map.of(
                "start_date", "模拟数据",
                "end_date", "模拟数据",
                "total_days", 0,
                "order_count", 0
        ));
        
        return prediction;
    }
} 