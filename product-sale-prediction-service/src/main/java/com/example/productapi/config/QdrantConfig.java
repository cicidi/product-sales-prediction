package com.example.productapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Collections;

@Configuration
public class QdrantConfig {

    @Value("${qdrant.host}")
    private String qdrantHost;

    @Value("${qdrant.grpc.port:6334}")
    private int qdrantGrpcPort;

    @Value("${qdrant.http.port:6333}")
    private int qdrantHttpPort;

    @Value("${qdrant.api-key}")
    private String qdrantApiKey;

    @Value("${qdrant.collection.name:product_embeddings}")
    private String collectionName;

    @Value("${qdrant.embedding.dimension:1536}")
    private int embeddingDimension;

    @Value("${qdrant.use.ssl:false}")
    private boolean useSSL;

    @Bean
    public QdrantClient qdrantClient() {
        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder
            .forAddress(qdrantHost, qdrantGrpcPort);
            
        if (!useSSL) {
            channelBuilder.usePlaintext();
        }

        ManagedChannel channel = channelBuilder.build();

        return new QdrantClient(
            QdrantGrpcClient.newBuilder(channel, useSSL)
                .withApiKey(qdrantApiKey)
                .build()
        );
    }

    @Bean
    public RestTemplate qdrantRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

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
        String protocol = useSSL ? "https" : "http";
        return protocol + "://" + qdrantHost + ":" + qdrantHttpPort;
    }

    @Bean
    public String qdrantCollectionName() {
        return collectionName;
    }
}