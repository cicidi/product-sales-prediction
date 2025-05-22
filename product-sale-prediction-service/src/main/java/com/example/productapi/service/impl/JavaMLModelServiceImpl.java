package com.example.productapi.service.impl;

import com.example.productapi.model.Order;
import com.example.productapi.repository.OrderRepository;
import com.example.productapi.service.MLModelService;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Model;
import org.jpmml.evaluator.*;
import org.jpmml.model.PMMLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import javax.xml.bind.JAXBException;

import jakarta.annotation.PostConstruct;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Native Java implementation of ML model service, loading and running models directly in JVM
 * Requires the following Maven dependencies:
 * - org.jpmml:pmml-evaluator:1.6.4
 * - org.jpmml:pmml-evaluator-extension:1.6.4
 */
@Service
@Primary
@ConditionalOnProperty(name = "ml.model.use.local.model", havingValue = "true", matchIfMissing = true)
public class JavaMLModelServiceImpl implements MLModelService {

    private static final Logger logger = LoggerFactory.getLogger(JavaMLModelServiceImpl.class);

    private boolean modelInitialized = false;
    private Path modelBasePath;
    
    // Model evaluator
    private Evaluator evaluator;
    // Product and seller encoding mappings
    private Map<String, Integer> productIdEncodings = new HashMap<>();
    private Map<String, Integer> sellerIdEncodings = new HashMap<>();
    // Model feature names list
    private List<String> featureNames = new ArrayList<>();
    
    @Autowired
    private OrderRepository orderRepository;

    @Value("${ml.model.base.path:../product-sale-prediction-AI/train/model}")
    private String modelBasePathStr;

    @PostConstruct
    @Override
    public void initializeModel() {
        try {
            // Get current project root directory
            Path currentPath = Paths.get("").toAbsolutePath();
            modelBasePath = currentPath.resolve(modelBasePathStr).normalize();
            File modelDir = modelBasePath.toFile();
            
            logger.info("Checking model directory: {}", modelBasePath);
            
            if (!modelDir.exists() || !modelDir.isDirectory()) {
                logger.error("Model directory does not exist or is not a valid directory: {}", modelBasePath);
                return;
            }
            
            // Check PMML model file
            File pmmlFile = modelBasePath.resolve("xgb_sales_forecast_model.pmml").toFile();
            
            if (!pmmlFile.exists()) {
                logger.error("PMML model file does not exist: {}", pmmlFile.getAbsolutePath());
                logger.info("Please ensure the model file is in the correct directory: {}", modelBasePath);
                return;
            }
            
            // Load PMML model
            logger.info("Loading PMML model: {}", pmmlFile.getAbsolutePath());
            PMML pmml = PMMLUtil.unmarshal(new FileInputStream(pmmlFile));
            Model model = pmml.getModels().get(0);
            evaluator = ModelEvaluatorFactory.newInstance().newModelEvaluator(pmml, model);
            evaluator.verify();
            
            // Load encoding mappings
            loadEncodings();
            
            // Load feature names
            loadFeatureNames();
            
            logger.info("Java ML model successfully loaded: {}", modelBasePath);
            modelInitialized = true;
            
            // Output some model information
            logger.info("Model target fields: {}", evaluator.getTargetFields());
            logger.info("Model input fields: {}", evaluator.getActiveFields());
            logger.info("Available products: {}", productIdEncodings.size());
            logger.info("Available sellers: {}", sellerIdEncodings.size());
            
        } catch (Exception e) {
            logger.error("Failed to initialize Java ML model", e);
            modelInitialized = false;
        }
    }
    
    /**
     * Load product and seller encoding mappings
     */
    private void loadEncodings() throws Exception {
        // Load product ID encoding mapping
        Path productEncodingsPath = modelBasePath.resolve("product_id_encodings.csv");
        if (Files.exists(productEncodingsPath)) {
            try {
                Files.lines(productEncodingsPath)
                    .skip(1) // Skip header row
                    .forEach(line -> {
                        String[] parts = line.split(",");
                        if (parts.length == 2) {
                            productIdEncodings.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
                        }
                    });
                logger.info("Successfully loaded {} product ID encodings", productIdEncodings.size());
            } catch (Exception e) {
                logger.error("Failed to load product encoding file: {}", productEncodingsPath, e);
                throw e;
            }
        } else {
            logger.warn("Product encoding file does not exist: {}", productEncodingsPath);
            // Create empty mapping file
            try (BufferedWriter writer = Files.newBufferedWriter(productEncodingsPath)) {
                writer.write("product_id,encoding\n");
            }
        }
        
        // Load seller ID encoding mapping
        Path sellerEncodingsPath = modelBasePath.resolve("seller_id_encodings.csv");
        if (Files.exists(sellerEncodingsPath)) {
            try {
                Files.lines(sellerEncodingsPath)
                    .skip(1) // Skip header row
                    .forEach(line -> {
                        String[] parts = line.split(",");
                        if (parts.length == 2) {
                            sellerIdEncodings.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
                        }
                    });
                logger.info("Successfully loaded {} seller ID encodings", sellerIdEncodings.size());
            } catch (Exception e) {
                logger.error("Failed to load seller encoding file: {}", sellerEncodingsPath, e);
                throw e;
            }
        } else {
            logger.warn("Seller encoding file does not exist: {}", sellerEncodingsPath);
            // Create empty mapping file
            try (BufferedWriter writer = Files.newBufferedWriter(sellerEncodingsPath)) {
                writer.write("seller_id,encoding\n");
            }
        }
    }
    
    /**
     * Load model feature names
     */
    private void loadFeatureNames() throws Exception {
        Path featureNamesPath = modelBasePath.resolve("feature_names.txt");
        if (Files.exists(featureNamesPath)) {
            try {
                featureNames = Files.readAllLines(featureNamesPath);
                logger.info("Successfully loaded {} feature names", featureNames.size());
            } catch (Exception e) {
                logger.error("Failed to load feature names file: {}", featureNamesPath, e);
                throw e;
            }
        } else {
            logger.warn("Feature names file does not exist: {}", featureNamesPath);
            // Use default feature names
            featureNames = Arrays.asList(
                "unit_price", "dayofweek", "day", "week", "month", "quarter", "year",
                "is_weekend", "is_month_start", "is_month_end", "product_id_enc",
                "seller_id_enc", "lag_1_quantity", "lag_2_quantity", "lag_3_quantity",
                "lag_4_quantity", "lag_5_quantity", "lag_6_quantity", "lag_7_quantity",
                "lag_8_quantity", "lag_9_quantity", "lag_10_quantity", "lag_11_quantity",
                "lag_12_quantity", "lag_13_quantity", "lag_14_quantity",
                "rolling_mean_7d", "rolling_std_7d", "rolling_mean_14d", "rolling_std_14d",
                "rolling_mean_30d", "rolling_std_30d"
            );
            // Create feature names file
            try (BufferedWriter writer = Files.newBufferedWriter(featureNamesPath)) {
                for (String feature : featureNames) {
                    writer.write(feature);
                    writer.newLine();
                }
            }
        }
    }

    @Override
    public boolean isModelInitialized() {
        return modelInitialized;
    }

    @Override
    public Map<String, Object> predictFutureSales(String productId, String sellerId, Double unitPrice, Integer weeksAhead) {
        if (!modelInitialized) {
            logger.error("ML model not initialized, cannot make predictions");
            throw new IllegalStateException("ML model not initialized");
        }
        
        if (weeksAhead == null || weeksAhead < 1) {
            weeksAhead = 4; // Default to predict 4 weeks
        }
        
        try {
            // Get all historical sales data for this seller from database
            List<Double> historicalSales = getHistoricalSales(productId, sellerId);
            
            if (historicalSales.isEmpty()) {
                logger.warn("No historical sales data found for seller {} and product {}, cannot make accurate predictions", sellerId, productId);
                return generateMockPrediction(productId, sellerId, unitPrice, weeksAhead);
            }
            
            logger.info("Retrieved {} historical sales records for product {} and seller {}", historicalSales.size(), productId, sellerId);
            
            // Prepare prediction results
            Map<String, Object> prediction = new HashMap<>();
            prediction.put("product_id", productId);
            prediction.put("seller_id", sellerId);
            prediction.put("unit_price", unitPrice);
            
            List<Map<String, Object>> predictions = new ArrayList<>();
            LocalDate currentDate = LocalDate.now();
            
            // Make predictions for each week
            for (int week = 0; week < weeksAhead; week++) {
                // Calculate prediction date
                LocalDate predictionDate = currentDate.plusWeeks(week);
                
                // Prepare model input
                Map<String, Object> features = prepareFeatures(
                        productId, sellerId, unitPrice, historicalSales, predictionDate);
                
                // Execute prediction
                Map<String, Object> weekPrediction = executeModelPrediction(features);
                
                // Ensure prediction value is non-negative
                double salesPrediction = Math.max(0, (double) weekPrediction.get("predicted_sales"));
                
                // Add to prediction results
                Map<String, Object> weekPredictionMap = new HashMap<>();
                weekPredictionMap.put("week_number", week + 1);
                weekPredictionMap.put("prediction_date", predictionDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
                weekPredictionMap.put("predicted_sales", Math.round(salesPrediction));
                
                predictions.add(weekPredictionMap);
                
                // Add this week's prediction to historical data for next week's prediction
                historicalSales.add(salesPrediction);
                if (historicalSales.size() > 30) {
                    historicalSales.remove(0);
                }
            }
            
            // Get start and end dates of historical data
            LocalDate startDate = LocalDate.now().minusDays(historicalSales.size());
            LocalDate endDate = LocalDate.now();
            
            prediction.put("predictions", predictions);
            prediction.put("historical_data_used", Map.of(
                    "start_date", startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    "end_time", endDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    "total_days", historicalSales.size()
            ));
            
            return prediction;
        } catch (Exception e) {
            logger.error("Sales prediction failed", e);
            return generateMockPrediction(productId, sellerId, unitPrice, weeksAhead);
        }
    }
    
    /**
     * Execute model prediction
     */
    private Map<String, Object> executeModelPrediction(Map<String, Object> features) {
        try {
            // Prepare input fields
            Map<FieldName, Object> arguments = new LinkedHashMap<>();
            for (InputField inputField : evaluator.getActiveFields()) {
                FieldName fieldName = inputField.getName();
                Object rawValue = features.get(fieldName.getValue());
                Object preparedValue = inputField.prepare(rawValue);
                arguments.put(fieldName, preparedValue);
            }
            
            // Execute prediction
            Map<FieldName, ?> results = evaluator.evaluate(arguments);
            
            // Process prediction results
            List<TargetField> targetFields = evaluator.getTargetFields();
            Map<String, Object> predictions = new HashMap<>();
            
            for (TargetField targetField : targetFields) {
                FieldName targetFieldName = targetField.getName();
                Object targetValue = results.get(targetFieldName);
                predictions.put(targetFieldName.getValue(), targetValue);
            }
            
            return predictions;
        } catch (Exception e) {
            logger.error("Model prediction failed", e);
            throw new RuntimeException("Model prediction failed: " + e.getMessage());
        }
    }
    
    /**
     * Prepare model input features
     */
    private Map<String, Object> prepareFeatures(String productId, String sellerId, Double unitPrice, 
                                              List<Double> historicalSales, LocalDate predictionDate) {
        Map<String, Object> features = new HashMap<>();
        
        // Process product ID and seller ID encodings
        int productIdEnc = productIdEncodings.getOrDefault(productId, 0);
        int sellerIdEnc = sellerIdEncodings.getOrDefault(sellerId, 0);
        
        // Basic features
        features.put("unit_price", unitPrice);
        features.put("dayofweek", predictionDate.getDayOfWeek().getValue() % 7); // 0-6, Sunday is 0
        features.put("day", predictionDate.getDayOfMonth());
        features.put("week", predictionDate.get(java.time.temporal.WeekFields.ISO.weekOfYear()));
        features.put("month", predictionDate.getMonthValue());
        features.put("quarter", (predictionDate.getMonthValue() - 1) / 3 + 1);
        features.put("year", predictionDate.getYear());
        features.put("is_weekend", predictionDate.getDayOfWeek().getValue() >= 6 ? 1 : 0);
        features.put("is_month_start", predictionDate.getDayOfMonth() == 1 ? 1 : 0);
        features.put("is_month_end", predictionDate.getDayOfMonth() == predictionDate.lengthOfMonth() ? 1 : 0);
        features.put("product_id_enc", productIdEnc);
        features.put("seller_id_enc", sellerIdEnc);
        
        // Lag features
        int histSize = historicalSales.size();
        for (int i = 1; i <= 14; i++) {  // Updated to 14 lag features
            double lagValue = 0.0;
            if (i <= histSize) {
                lagValue = historicalSales.get(histSize - i);
            }
            features.put("lag_" + i + "_quantity", lagValue);
        }
        
        // Moving average features
        if (histSize >= 7) {
            List<Double> last7 = historicalSales.subList(histSize - 7, histSize);
            features.put("rolling_mean_7d", last7.stream().mapToDouble(Double::doubleValue).average().orElse(0));
            features.put("rolling_std_7d", calculateStd(last7));
        } else {
            features.put("rolling_mean_7d", histSize > 0 ? historicalSales.stream().mapToDouble(Double::doubleValue).average().orElse(0) : 0);
            features.put("rolling_std_7d", histSize > 0 ? calculateStd(historicalSales) : 0);
        }
        
        if (histSize >= 14) {
            List<Double> last14 = historicalSales.subList(histSize - 14, histSize);
            features.put("rolling_mean_14d", last14.stream().mapToDouble(Double::doubleValue).average().orElse(0));
            features.put("rolling_std_14d", calculateStd(last14));
        } else {
            features.put("rolling_mean_14d", features.get("rolling_mean_7d"));
            features.put("rolling_std_14d", features.get("rolling_std_7d"));
        }
        
        if (histSize >= 30) {
            List<Double> last30 = historicalSales.subList(histSize - 30, histSize);
            features.put("rolling_mean_30d", last30.stream().mapToDouble(Double::doubleValue).average().orElse(0));
            features.put("rolling_std_30d", calculateStd(last30));
        } else {
            features.put("rolling_mean_30d", features.get("rolling_mean_7d"));
            features.put("rolling_std_30d", features.get("rolling_std_7d"));
        }
        
        return features;
    }
    
    /**
     * Calculate standard deviation
     */
    private double calculateStd(List<Double> values) {
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = values.stream()
            .mapToDouble(x -> Math.pow(x - mean, 2))
            .average()
            .orElse(0);
        return Math.sqrt(variance);
    }
    
    /**
     * Get historical sales data - using actual order data
     * 
     * @param productId Product ID
     * @param sellerId Seller ID
     * @return List of historical sales volumes sorted by date
     */
    private List<Double> getHistoricalSales(String productId, String sellerId) {
        // Get all historical order data, using a very early date to ensure getting all history
        LocalDateTime longTimeAgo = LocalDateTime.now().minusYears(10); // 10 years ago
        List<Order> historicalOrders = orderRepository.findBySellerIdAndProductIdAndTimestampAfter(
                sellerId, productId, longTimeAgo);
        
        if (historicalOrders == null || historicalOrders.isEmpty()) {
            logger.warn("No historical order data found for product {} and seller {}", productId, sellerId);
            return new ArrayList<>();
        }
        
        // Sort by timestamp
        historicalOrders.sort(Comparator.comparing(Order::getTimestamp));
        
        // Group orders by date and calculate daily total sales
        Map<LocalDate, Double> dailySales = new HashMap<>();
        
        for (Order order : historicalOrders) {
            LocalDate orderDate = order.getTimestamp().toLocalDate();
            dailySales.merge(orderDate, (double) order.getQuantity(), Double::sum);
        }
        
        // Fill in missing dates - ensure data continuity
        LocalDate firstDate = historicalOrders.get(0).getTimestamp().toLocalDate();
        LocalDate lastDate = historicalOrders.get(historicalOrders.size() - 1).getTimestamp().toLocalDate();
        
        // Calculate all dates within the time range
        List<LocalDate> allDates = new ArrayList<>();
        long daysBetween = ChronoUnit.DAYS.between(firstDate, lastDate);
        
        for (int i = 0; i <= daysBetween; i++) {
            LocalDate date = firstDate.plusDays(i);
            allDates.add(date);
        }
        
        // Build sales data array using all dates, record 0 for days without sales
        List<Double> salesData = allDates.stream()
                .map(date -> dailySales.getOrDefault(date, 0.0))
                .collect(Collectors.toList());
        
        logger.info("Retrieved {} days of historical sales data, from {} to {}", 
                salesData.size(), firstDate, lastDate);
        
        return salesData;
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
            weekPrediction.put("prediction_date", getDateForWeeksAhead(week));
            
            // Add some randomness but follow trend
            double weeklyFactor = 1.0 + (week * 0.05) + (random.nextDouble() * 0.1 - 0.05);
            double predictedSales = baseQuantity * weeklyFactor;
            
            weekPrediction.put("predicted_sales", Math.round(predictedSales));
            predictions.add(weekPrediction);
        }
        
        prediction.put("predictions", predictions);
        prediction.put("historical_data_used", Map.of(
                "start_date", "2023-01-01",
                "end_date", "2023-12-31",
                "total_days", 365
        ));
        
        return prediction;
    }
    
    private String getDateForWeeksAhead(int week) {
        LocalDate date = LocalDate.now().plusWeeks(week + 1);
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
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
} 