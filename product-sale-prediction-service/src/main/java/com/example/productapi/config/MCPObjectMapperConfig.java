package com.example.productapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MCPObjectMapperConfig {

    @Bean
    @Primary
    public ObjectMapper mcpObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        // Configure output as camelCase
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
        return objectMapper;
    }

    @Bean(name = "snakeCaseObjectMapper")
    public ObjectMapper snakeCaseObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        // Configure output as snake_case
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return objectMapper;
    }
} 