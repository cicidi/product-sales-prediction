package com.example.productapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class EnvConfig {

    @Bean
    public Map<String, String> envVariables(Environment env) {
        Map<String, String> envMap = new HashMap<>();
        
        // Try to load from .env file in user's home directory
        String userHome = System.getProperty("user.home");
        Path envPath = Paths.get(userHome, ".env");
        
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
                        envMap.put(key, value);
                        // Also set as system property for Spring to pick up
                        System.setProperty(key, value);
                    }
                }
            }
        } catch (IOException e) {
            // Log warning but don't fail - environment variables might be set another way
            System.out.println("Warning: Could not load .env file from " + envPath);
        }
        
        return envMap;
    }
} 