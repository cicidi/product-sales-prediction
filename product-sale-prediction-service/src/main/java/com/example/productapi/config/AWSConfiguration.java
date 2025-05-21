package com.example.productapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sagemakerruntime.SageMakerRuntimeClient;

@Configuration
public class AWSConfiguration {

    @Value("${aws.sagemaker.endpoint-name:sales-predict-endpoint}")
    private String endpointName;
    
    @Value("${aws.region:us-west-2}")
    private String region;
    
    @Value("${aws.access-key:AKIASGXFHFEHB6SBIW6H}")
    private String accessKey;
    
    @Value("${aws.secret-key:v574sKnXSBESFaNmivkerwV04uZ1xBF90Ac856cm}")
    private String secretKey;
    
    @Bean
    public String sagemakerEndpointName() {
        return endpointName;
    }
    
    @Bean
    public SageMakerRuntimeClient sageMakerRuntimeClient() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        
        return SageMakerRuntimeClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
} 