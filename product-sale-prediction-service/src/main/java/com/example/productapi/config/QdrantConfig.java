package com.example.productapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class QdrantConfig {

    @Value("${qdrant.host:bcdb7803-3764-46a7-9d0e-d115a81f8ed9.europe-west3-0.gcp.cloud.qdrant.io}")
    private String qdrantHost;

    @Value("${qdrant.port:6333}")
    private int qdrantPort;

    @Value("${qdrant.api-key:eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhY2Nlc3MiOiJtIn0.AIQURDWwgjJ3gpl6lZ_ppDz_m6kYK8nan--LVVPubPo}")
    private String qdrantApiKey;

    @Value("${qdrant.collection.name:product_embeddings}")
    private String collectionName;

    @Value("${qdrant.embedding.dimension:1536}")
    private int embeddingDimension;

    @Bean
    public RestTemplate qdrantRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // Add API key as interceptor
        ClientHttpRequestInterceptor apiKeyInterceptor = (request, body, execution) -> {
            HttpHeaders headers = request.getHeaders();
            headers.set("api-key", qdrantApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            return execution.execute(request, body);
        };

        restTemplate.setInterceptors(Collections.singletonList(apiKeyInterceptor));
        return restTemplate;
    }

    @Bean
    public String qdrantBaseUrl() {
        return "https://" + qdrantHost + ":" + qdrantPort;
    }

    @Bean
    public String qdrantCollectionName() {
        return collectionName;
    }
}