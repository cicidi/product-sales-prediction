package com.example.productapi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EmbeddingService {

    private final RestTemplate qdrantRestTemplate;
    private final String qdrantBaseUrl;
    private final String collectionName;
    private final Map<String, List<Float>> embeddingCache = new ConcurrentHashMap<>();

    @Value("${openai.api.key:}")
    private String openaiApiKey;

    @Value("${openai.model:text-embedding-ada-002}")
    private String openaiModel;

    @Value("${qdrant.embedding.dimension:1536}")
    private int embeddingDimension;

    @Autowired
    public EmbeddingService(RestTemplate qdrantRestTemplate,
                          String qdrantBaseUrl,
                          String qdrantCollectionName) {
        this.qdrantRestTemplate = qdrantRestTemplate;
        this.qdrantBaseUrl = qdrantBaseUrl;
        this.collectionName = qdrantCollectionName;

        // Ensure the collection exists
        try {
            createCollectionIfNotExists();
            log.info("Initialized EmbeddingService with Qdrant at {}", qdrantBaseUrl);
        } catch (Exception e) {
            log.error("Error initializing Qdrant collection: {}", e.getMessage());
        }
    }

    /**
     * Create Qdrant collection if it doesn't exist
     */
    private void createCollectionIfNotExists() {
        String collectionsUrl = String.format("%s/collections", qdrantBaseUrl);

        try {
            // Try to get collection to check if it exists
            String collectionUrl = String.format("%s/collections/%s", qdrantBaseUrl, collectionName);
            ResponseEntity<Map> response = qdrantRestTemplate.getForEntity(collectionUrl, Map.class);
            log.info("Qdrant collection exists: {}", collectionName);
        } catch (Exception e) {
            // Collection doesn't exist, create it
            log.info("Creating Qdrant collection: {}", collectionName);

            Map<String, Object> createRequest = new HashMap<>();
            Map<String, Object> vectorsConfig = new HashMap<>();
            vectorsConfig.put("size", embeddingDimension);
            vectorsConfig.put("distance", "Cosine");
            createRequest.put("name", collectionName);
            createRequest.put("vectors_config", vectorsConfig);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(createRequest);
            qdrantRestTemplate.exchange(collectionsUrl, HttpMethod.PUT, entity, Map.class);
            log.info("Created Qdrant collection: {}", collectionName);
        }
    }

    /**
     * Generate embeddings for text using OpenAI API
     * For simplicity, we're using a mock implementation that returns random vectors
     */
    public List<Float> generateEmbedding(String text) {
        // In a real implementation, we'd call OpenAI API
        // For mock purposes, we'll generate a random vector
        List<Float> embedding = new ArrayList<>();
        for (int i = 0; i < embeddingDimension; i++) {
            embedding.add((float) Math.random());
        }
        return embedding;
    }

    /**
     * Save product embedding to Qdrant
     */
    public void saveProductEmbedding(String productId, List<Float> embedding) {
        try {
            // Store in cache
            embeddingCache.put(productId, embedding);

            // Create a point for Qdrant
            Map<String, Object> point = new HashMap<>();
            point.put("id", productId);
            point.put("vector", embedding);
            point.put("payload", Map.of("product_id", productId));

            // Prepare the request
            String pointsUrl = String.format("%s/collections/%s/points", qdrantBaseUrl, collectionName);
            Map<String, Object> upsertRequest = new HashMap<>();
            upsertRequest.put("points", List.of(point));

            // Send the request
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(upsertRequest);
            qdrantRestTemplate.exchange(pointsUrl, HttpMethod.PUT, requestEntity, Map.class);

            log.debug("Saved embedding for product {} to Qdrant", productId);
        } catch (Exception e) {
            log.error("Error saving embedding for product {} to Qdrant: {}", productId, e.getMessage());
        }
    }

    /**
     * Find similar products based on embedding similarity
     */
    public List<String> findSimilarProducts(List<Float> embedding, int limit) {
        try {
            // Prepare search request
            String searchUrl = String.format("%s/collections/%s/points/search", qdrantBaseUrl, collectionName);

            Map<String, Object> searchRequest = new HashMap<>();
            searchRequest.put("vector", embedding);
            searchRequest.put("limit", limit);

            // Send search request
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(searchRequest);
            ResponseEntity<Map> response = qdrantRestTemplate.exchange(
                searchUrl, HttpMethod.POST, requestEntity, Map.class);

            // Parse results
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("result");

            // Extract product IDs
            return results.stream()
                .map(result -> {
                    Map<String, Object> payload = (Map<String, Object>) result.get("payload");
                    return payload.get("product_id").toString();
                })
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error searching similar products in Qdrant: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get embedding for a product ID
     */
    public List<Float> getProductEmbedding(String productId) {
        // Check cache first
        if (embeddingCache.containsKey(productId)) {
            return embeddingCache.get(productId);
        }

        try {
            // Prepare request to get a point
            String pointsUrl = String.format("%s/collections/%s/points/%s",
                qdrantBaseUrl, collectionName, productId);

            // Send request
            ResponseEntity<Map> response = qdrantRestTemplate.getForEntity(pointsUrl, Map.class);
            Map<String, Object> result = (Map<String, Object>) response.getBody().get("result");

            // Extract vector
            if (result != null && result.containsKey("vector")) {
                List<Float> vector = (List<Float>) result.get("vector");
                embeddingCache.put(productId, vector);
                return vector;
            }
        } catch (Exception e) {
            log.error("Error retrieving embedding for product {} from Qdrant: {}", productId, e.getMessage());
        }

        return null;
    }
}