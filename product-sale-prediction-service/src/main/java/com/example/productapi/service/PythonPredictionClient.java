package com.example.productapi.service;

import com.example.productapi.dto.BatchRequest;
import com.example.productapi.dto.BatchResponse;
import com.example.productapi.dto.PythonPredictionRequest;
import com.example.productapi.dto.PythonPredictionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Service
public class PythonPredictionClient {
    
    private static final Logger logger = LoggerFactory.getLogger(PythonPredictionClient.class);
    
    private final WebClient webClient;
    private final String pythonServiceUrl;
    
    public PythonPredictionClient(@Value("${python.prediction.service.url:http://localhost:8000}") String pythonServiceUrl) {
        this.pythonServiceUrl = pythonServiceUrl;
        this.webClient = WebClient.builder()
                .baseUrl(pythonServiceUrl)
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(1024 * 1024)) // 1MB buffer
                .build();
        
        logger.info("Python Prediction Client initialized with URL: {}", pythonServiceUrl);
    }
    
    /**
     * Call Python prediction service for single prediction
     */
    public Double predictSingle(PythonPredictionRequest request) {
        try {
            logger.debug("Calling Python prediction service with request: {}", request);
            
            PythonPredictionResponse response = webClient
                    .post()
                    .uri("/predict")
                    .body(Mono.just(request), PythonPredictionRequest.class)
                    .retrieve()
                    .bodyToMono(PythonPredictionResponse.class)
                    .timeout(Duration.ofSeconds(10)) // 10 second timeout
                    .block();
            
            if (response != null && response.getPredictedQuantity() != null) {
                logger.debug("Python service returned prediction: {}", response.getPredictedQuantity());
                return response.getPredictedQuantity();
            } else {
                logger.warn("Python service returned null or invalid response: {}", response);
                return null;
            }
            
        } catch (WebClientResponseException e) {
            logger.error("Python prediction service HTTP error - Status: {}, Body: {}", 
                    e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            logger.error("Error calling Python prediction service: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Call Python prediction service for batch prediction - more efficient for multiple predictions
     */
    public List<Double> predictBatch(List<PythonPredictionRequest> requests) {
        try {
            logger.debug("Calling Python batch prediction service with {} requests", requests.size());
            
            BatchRequest batchRequest = new BatchRequest(requests);
            
            BatchResponse response = webClient
                    .post()
                    .uri("/predict/batch")
                    .body(Mono.just(batchRequest), BatchRequest.class)
                    .retrieve()
                    .bodyToMono(BatchResponse.class)
                    .timeout(Duration.ofSeconds(30)) // Longer timeout for batch requests
                    .block();
            
            if (response != null && response.getPredictions() != null && !response.getPredictions().isEmpty()) {
                logger.debug("Python service returned {} batch predictions", response.getPredictions().size());
                return response.getPredictions();
            } else {
                logger.warn("Python service returned null or empty batch response: {}", response);
                return null;
            }
            
        } catch (WebClientResponseException e) {
            logger.error("Python batch prediction service HTTP error - Status: {}, Body: {}", 
                    e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            logger.error("Error calling Python batch prediction service: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Check if Python prediction service is available
     */
    public boolean isServiceAvailable() {
        try {
            String response = webClient
                    .get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            
            logger.debug("Python service health check response: {}", response);
            return response != null;
            
        } catch (Exception e) {
            logger.warn("Python prediction service is not available: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get service URL for logging/debugging
     */
    public String getServiceUrl() {
        return pythonServiceUrl;
    }
} 