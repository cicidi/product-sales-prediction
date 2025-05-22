package com.example.productapi.service.impl;

import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;

import com.example.productapi.service.QdrantService;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.JsonWithInt.Value;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.SearchPoints;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QdrantServiceImpl implements QdrantService {

  private final QdrantClient client;
  private final String collectionName;
  private final Map<String, Long> pointIdMapping = new ConcurrentHashMap<>();
  private long nextId = 1;

  public QdrantServiceImpl(
      @org.springframework.beans.factory.annotation.Value("${qdrant.host}") String host,
      @org.springframework.beans.factory.annotation.Value("${qdrant.grpc.port}") int port,
      @org.springframework.beans.factory.annotation.Value("${qdrant.api-key}") String apiKey,
      @org.springframework.beans.factory.annotation.Value("${qdrant.collection.name}") String collectionName) {

    // Set up API key metadata
    Metadata metadata = new Metadata();
    Metadata.Key<String> apiKeyHeader = Metadata.Key.of("api-key",
        Metadata.ASCII_STRING_MARSHALLER);
    metadata.put(apiKeyHeader, apiKey);

    // Create channel with TLS and API key
    var channel = ManagedChannelBuilder.forAddress(host, port)
        .useTransportSecurity() // Enable TLS
        .intercept(MetadataUtils.newAttachHeadersInterceptor(metadata))
        .build();

    this.client = new QdrantClient(QdrantGrpcClient.newBuilder(channel).build());
    this.collectionName = collectionName;
    initializeCollection();
  }

  private void initializeCollection() {
    try {
      // Create collection if it doesn't exist
      client.createCollectionAsync(
              collectionName,
              VectorParams.newBuilder()
                  .setDistance(Distance.Cosine)
                  .setSize(1536) // OpenAI embedding dimension
                  .build())
          .get();
      log.info("Created Qdrant collection: {}", collectionName);
    } catch (Exception e) {
      log.info("Collection {} already exists or error occurred: {}", collectionName,
          e.getMessage());
    }
  }

  private long getOrCreateNumericId(String pointId) {
    return pointIdMapping.computeIfAbsent(pointId, k -> nextId++);
  }

  @Override
  public void upsertPoint(String pointId, List<Float> vector, Map<String, Object> payload) {
    try {
      long numericId = getOrCreateNumericId(pointId);
      Map<String, Object> updatedPayload = new HashMap<>(payload);
      updatedPayload.put("original_id", pointId);

      PointStruct point = PointStruct.newBuilder()
          .setId(id(numericId))
          .setVectors(vectors(vector))
          .putAllPayload(convertToValueMap(updatedPayload))
          .build();

      client.upsertAsync(collectionName, List.of(point))
          .get();
      log.debug("Upserted point {} (numeric ID: {}) to collection {}", pointId, numericId,
          collectionName);
    } catch (Exception e) {
      log.error("Error upserting point to Qdrant: {}", e.getMessage());
      throw new RuntimeException("Failed to upsert point to Qdrant", e);
    }
  }

  @Override
  public void deletePoint(String pointId) {
    try {
      Long numericId = pointIdMapping.remove(pointId);
      if (numericId != null) {
        client.deleteAsync(collectionName, List.of(id(numericId)))
            .get();
        log.debug("Deleted point {} (numeric ID: {}) from collection {}", pointId, numericId,
            collectionName);
      } else {
        log.warn("Point {} not found in ID mapping", pointId);
      }
    } catch (Exception e) {
      log.error("Error deleting point from Qdrant: {}", e.getMessage());
      throw new RuntimeException("Failed to delete point from Qdrant", e);
    }
  }

  @Override
  public List<Map<String, Object>> findNearest(List<Float> vector, int limit) {
    try {
      var searchResults = client.searchAsync(
              SearchPoints.newBuilder()
                  .setCollectionName(collectionName)
                  .addAllVector(vector)
                  .setLimit(limit)
                  .build())
          .get();

      return searchResults.stream()
          .map(result -> {
            Map<String, Object> payload = new HashMap<>(result.getPayloadMap());
            payload.put("score", result.getScore());
            return payload;
          })
          .collect(Collectors.toList());
    } catch (Exception e) {
      log.error("Error searching nearest points in Qdrant: {}", e.getMessage());
      throw new RuntimeException("Failed to search nearest points in Qdrant", e);
    }
  }

  private Map<String, Value> convertToValueMap(Map<String, Object> payload) {
    Map<String, Value> valueMap = new HashMap<>();
    payload.forEach((key, obj) -> {
      if (obj instanceof String) {
        valueMap.put(key, value((String) obj));
      } else if (obj instanceof Number) {
        valueMap.put(key, value(((Number) obj).doubleValue()));
      } else if (obj instanceof Boolean) {
        valueMap.put(key, value((Boolean) obj));
      }
    });
    return valueMap;
  }
}