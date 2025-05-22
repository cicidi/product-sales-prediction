package com.example.productapi.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@Configuration
public class ApiKeyConfig {
    private static final Logger log = LoggerFactory.getLogger(ApiKeyConfig.class);
    
    @Autowired
    private Environment environment;
    
    @PostConstruct
    public void init() {
        String userHome = System.getProperty("user.home");
        Path envPath = Paths.get(userHome, ".env");
        
        Properties props = new Properties();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(envPath.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        String value = parts[1].trim();
                        // Remove quotes if present
                        if (value.startsWith("\"") && value.endsWith("\"")) {
                            value = value.substring(1, value.length() - 1);
                        }
                        // Set as system property
                        System.setProperty(key, value);
                        props.setProperty(key, value);
                        
                        if (key.equals("OPENAI_API_KEY") || key.equals("QDRANT_API_KEY")) {
                            log.info("Successfully loaded {} from .env file", key);
                        }
                    }
                }
            }
            
            // Verify required keys
            verifyApiKey("OPENAI_API_KEY", props);
            verifyApiKey("QDRANT_API_KEY", props);
            
        } catch (IOException e) {
            log.error("Error reading .env file from {}: {}", envPath, e.getMessage());
            throw new RuntimeException("Failed to load API keys from .env file", e);
        }
    }
    
    private void verifyApiKey(String key, Properties props) {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            log.error("{} not found in .env file", key);
            throw new RuntimeException(key + " is required but not found in .env file");
        }
    }
} 