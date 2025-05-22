package com.example.productapi.controller;

import com.example.productapi.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/test")
@Slf4j
public class TestController {

    private final EmbeddingService embeddingService;

    @Autowired
    public TestController(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @PostMapping("/embedding")
    public ResponseEntity<?> testEmbedding(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Text is required");
            }

            List<Float> embedding = embeddingService.generateEmbedding(text);
            if (embedding == null) {
                return ResponseEntity.internalServerError().body("Failed to generate embedding");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("text", text);
            response.put("embedding", embedding);
            response.put("dimension", embedding.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in test embedding: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/store")
    public ResponseEntity<?> testStore(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            String id = request.get("id");
            
            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Text is required");
            }
            if (id == null || id.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("ID is required");
            }

            embeddingService.storeEmbedding(id, text);
            return ResponseEntity.ok().body("Successfully stored embedding for ID: " + id);
        } catch (Exception e) {
            log.error("Error in test store: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/similar")
    public ResponseEntity<?> testSimilar(@RequestParam String text, @RequestParam(defaultValue = "5") int limit) {
        try {
            List<String> similarIds = embeddingService.findSimilarProducts(text, limit);
            Map<String, Object> response = new HashMap<>();
            response.put("query", text);
            response.put("similar_ids", similarIds);
            response.put("count", similarIds.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in test similar: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/config")
    public ResponseEntity<?> testConfig() {
        try {
            String openaiKey = System.getProperty("OPENAI_API_KEY");
            String qdrantKey = System.getProperty("QDRANT_API");
            
            Map<String, Object> response = new HashMap<>();
            response.put("openai_key_loaded", openaiKey != null && !openaiKey.isEmpty());
            response.put("qdrant_key_loaded", qdrantKey != null && !qdrantKey.isEmpty());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in test config: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
} 