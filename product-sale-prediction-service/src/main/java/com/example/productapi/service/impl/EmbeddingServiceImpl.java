package com.example.productapi.service.impl;

import com.example.productapi.service.EmbeddingService;
import com.example.productapi.service.QdrantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EmbeddingServiceImpl implements EmbeddingService {

  private final QdrantService qdrantService;
  private final Map<String, List<Float>> embeddingCache = new ConcurrentHashMap<>();
  private final EmbeddingClient embeddingClient;

  @Autowired
  public EmbeddingServiceImpl(QdrantService qdrantService, EmbeddingClient embeddingClient) {
    this.qdrantService = qdrantService;
    this.embeddingClient = embeddingClient;
    log.info("Initialized EmbeddingService with OpenAI client");
  }

  @Override
  public List<Float> generateEmbedding(String text) {
    try {
      EmbeddingResponse response = embeddingClient.embedForResponse(List.of(text));
      List<Double> embedding = response.getResults().get(0).getOutput();
      return embedding.stream().map(d -> d.floatValue()).collect(Collectors.toList());
    } catch (Exception e) {
      log.error("Error generating embedding from OpenAI: {}", e.getMessage());
      return null;
    }
  }

  @Override
  public void storeEmbedding(String productId, String text) {
    try {
      List<Float> embedding = generateEmbedding(text);
      if (embedding != null) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("product_id", productId);
        payload.put("text", text);

        qdrantService.upsertPoint(productId, embedding, payload);
        embeddingCache.put(productId, embedding);
        log.info("Successfully stored embedding for product {}", productId);
      }
    } catch (Exception e) {
      log.error("Error storing embedding for product {}: {}", productId, e.getMessage());
    }
  }

  @Override
  public List<Float> getEmbedding(String productId) {
    return embeddingCache.get(productId);
  }

  @Override
  public void deleteEmbedding(String productId) {
    try {
      qdrantService.deletePoint(productId);
      embeddingCache.remove(productId);
      log.info("Successfully deleted embedding for product {}", productId);
    } catch (Exception e) {
      log.error("Error deleting embedding for product {}: {}", productId, e.getMessage());
    }
  }

  @Override
  public List<String> findSimilarProducts(String text, int limit) {
    try {
      List<Float> queryEmbedding = generateEmbedding(text);
      if (queryEmbedding != null) {
        return qdrantService.findNearest(queryEmbedding, limit).stream()
            .map(result -> (String) result.get("product_id")).collect(Collectors.toList());
      }
    } catch (Exception e) {
      log.error("Error finding similar products: {}", e.getMessage());
    }
    return new ArrayList<>();
  }
} 