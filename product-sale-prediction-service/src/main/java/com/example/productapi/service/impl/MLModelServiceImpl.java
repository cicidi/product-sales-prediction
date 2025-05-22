package com.example.productapi.service.impl;

import com.example.productapi.service.MLModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.springframework.context.annotation.Primary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Service
@Primary
@ConditionalOnProperty(name = "ml.model.use.local.model", havingValue = "false")
public class MLModelServiceImpl implements MLModelService {
    
    private static final Logger logger = LoggerFactory.getLogger(MLModelServiceImpl.class);
    
    private boolean modelInitialized = false;
    private Path modelBasePath;
    
    @Value("${ml.model.base.path:../product-sale-prediction-AI/train/model}")
    private String modelBasePathStr;
    
    @Value("${ml.model.use.local.model:true}")
    private boolean useLocalModel;
    
    @Value("${ml.model.api.endpoint:http://localhost:8000/predict}")
    private String modelApiEndpoint;
    
    private final RestTemplate restTemplate;
    
    public MLModelServiceImpl() {
        this.restTemplate = new RestTemplate();
    }
    
    @PostConstruct
    @Override
    public void initializeModel() {
        try {
            modelBasePath = Paths.get(modelBasePathStr).toAbsolutePath();
            File modelDir = modelBasePath.toFile();
            
            if (!modelDir.exists() || !modelDir.isDirectory()) {
                logger.error("Model directory does not exist: {}", modelBasePath);
                return;
            }
            
            // Check for required model files
            File modelFile = modelBasePath.resolve("xgb_sales_forecast_model.joblib").toFile();
            File productEncoderFile = modelBasePath.resolve("label_encoder_product_id.joblib").toFile();
            File sellerEncoderFile = modelBasePath.resolve("label_encoder_seller_id.joblib").toFile();
            
            if (!modelFile.exists() || !productEncoderFile.exists() || !sellerEncoderFile.exists()) {
                logger.error("Required model files are missing in directory: {}", modelBasePath);
                return;
            }
            
            logger.info("ML Model files validated successfully at: {}", modelBasePath);
            modelInitialized = true;
            
        } catch (Exception e) {
            logger.error("Failed to initialize ML model", e);
            modelInitialized = false;
        }
    }
    
    @Override
    public boolean isModelInitialized() {
        return modelInitialized;
    }
    
    @Override
    public Map<String, Object> predictFutureSales(String productId, String sellerId, Double unitPrice, Integer weeksAhead) {
        if (!modelInitialized && useLocalModel) {
            logger.error("ML Model is not initialized, cannot make predictions");
            throw new IllegalStateException("ML Model is not initialized");
        }
        
        if (weeksAhead == null || weeksAhead < 1) {
            weeksAhead = 4; // Default: predict for 4 weeks
        }
        
        try {
            if (useLocalModel) {
                // For the initial implementation, we'll use a simple HTTP call to the Python model server
                // In a real-world scenario, you could use JavaCPP or other libraries to load the model directly
                return callPythonModelApi(productId, sellerId, unitPrice, weeksAhead);
            } else {
                // Mock prediction for now, will be replaced with real ML model integration
                return generateMockPrediction(productId, sellerId, unitPrice, weeksAhead);
            }
        } catch (Exception e) {
            logger.error("Error making sales prediction for product {} and seller {}", productId, sellerId, e);
            throw new RuntimeException("Error predicting sales: " + e.getMessage(), e);
        }
    }
    
    private Map<String, Object> callPythonModelApi(String productId, String sellerId, Double unitPrice, Integer weeksAhead) {
        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("product_id", productId);
        requestPayload.put("seller_id", sellerId);
        requestPayload.put("unit_price", unitPrice);
        requestPayload.put("number_of_week_in_future", weeksAhead);
        
        try {
            logger.info("Calling Python model API: {} with payload: {}", modelApiEndpoint, requestPayload);
            Map<String, Object> response = restTemplate.postForObject(
                    modelApiEndpoint, requestPayload, Map.class);
            
            if (response == null) {
                throw new RuntimeException("No response received from model API");
            }
            
            // Check if the response contains an error
            if (response.containsKey("error")) {
                throw new RuntimeException("Model API error: " + response.get("error"));
            }
            
            return response;
        } catch (Exception e) {
            logger.error("Failed to call model API", e);
            // Fallback to mock prediction
            logger.info("Falling back to mock prediction due to API error");
            return generateMockPrediction(productId, sellerId, unitPrice, weeksAhead);
        }
    }
    
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
            
            // Add some randomness but follow a trend
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
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.WEEK_OF_YEAR, week + 1);
        return String.format("%d-%02d-%02d", 
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH));
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