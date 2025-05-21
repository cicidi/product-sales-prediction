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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class QdrantService {

    private final RestTemplate qdrantRestTemplate;
    private final String qdrantBaseUrl;
    private final String collectionName;
    private final Map<String, List<Float>> embeddingCache = new ConcurrentHashMap<>();
    
    @Value("${qdrant.embedding.dimension:1536}")
    private int embeddingDimension;
    
    @Autowired
    public QdrantService(RestTemplate qdrantRestTemplate, 
                         String qdrantBaseUrl,
                         String qdrantCollectionName) {
        this.qdrantRestTemplate = qdrantRestTemplate;
        this.qdrantBaseUrl = qdrantBaseUrl;
        this.collectionName = qdrantCollectionName;
        
        // Ensure the collection exists
        try {
            createCollectionIfNotExists();
            log.info("Initialized QdrantService with Qdrant at {}", qdrantBaseUrl);
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
            createRequest.put("vectors", vectorsConfig);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(createRequest);
            qdrantRestTemplate.exchange(
                String.format("%s/collections/%s", collectionsUrl, collectionName), 
                HttpMethod.PUT, 
                entity, 
                Map.class
            );
            log.info("Created Qdrant collection: {}", collectionName);
        }
    }
    
    /**
     * Save vector to Qdrant
     */
    public void saveVector(String id, List<Float> vector, Map<String, Object> payload) {
        try {
            // Store in cache
            embeddingCache.put(id, vector);
            
            // Create a point for Qdrant
            Map<String, Object> point = new HashMap<>();
            point.put("id", id);
            point.put("vector", vector);
            point.put("payload", payload);
            
            // Prepare the request
            String pointsUrl = String.format("%s/collections/%s/points", qdrantBaseUrl, collectionName);
            Map<String, Object> upsertRequest = new HashMap<>();
            upsertRequest.put("points", List.of(point));
            
            // Send the request
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(upsertRequest);
            qdrantRestTemplate.exchange(pointsUrl, HttpMethod.PUT, requestEntity, Map.class);
            
            log.debug("Saved vector with ID {} to Qdrant", id);
        } catch (Exception e) {
            log.error("Error saving vector with ID {} to Qdrant: {}", id, e.getMessage());
        }
    }
    
    /**
     * Find similar vectors based on vector similarity
     */
    public List<Map<String, Object>> findSimilar(List<Float> vector, int limit) {
        try {
            // Prepare search request
            String searchUrl = String.format("%s/collections/%s/points/search", qdrantBaseUrl, collectionName);
            
            Map<String, Object> searchRequest = new HashMap<>();
            searchRequest.put("vector", vector);
            searchRequest.put("limit", limit);
            
            // Send search request
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(searchRequest);
            ResponseEntity<Map> response = qdrantRestTemplate.exchange(
                searchUrl, HttpMethod.POST, requestEntity, Map.class);
            
            // Parse results
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("result");
            return results;
                
        } catch (Exception e) {
            log.error("Error searching similar vectors in Qdrant: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Get vector by ID
     */
    public List<Float> getVector(String id) {
        // Check cache first
        if (embeddingCache.containsKey(id)) {
            return embeddingCache.get(id);
        }
        
        try {
            // Prepare request to get a point
            String pointsUrl = String.format("%s/collections/%s/points/%s", 
                qdrantBaseUrl, collectionName, id);
            
            // Send request
            ResponseEntity<Map> response = qdrantRestTemplate.getForEntity(pointsUrl, Map.class);
            Map<String, Object> result = (Map<String, Object>) response.getBody().get("result");
            
            // Extract vector
            if (result != null && result.containsKey("vector")) {
                List<Float> vector = (List<Float>) result.get("vector");
                embeddingCache.put(id, vector);
                return vector;
            }
        } catch (Exception e) {
            log.error("Error retrieving vector with ID {} from Qdrant: {}", id, e.getMessage());
        }
        
        return null;
    }
} 