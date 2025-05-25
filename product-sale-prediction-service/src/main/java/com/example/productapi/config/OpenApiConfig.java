package com.example.productapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    private final MCPOpenApiCustomizer mcpOpenApiCustomizer;

    public OpenApiConfig(MCPOpenApiCustomizer mcpOpenApiCustomizer) {
        this.mcpOpenApiCustomizer = mcpOpenApiCustomizer;
    }

    @Bean
    public OpenAPI productApiOpenAPI() {
        OpenAPI openAPI = new OpenAPI()
                .info(new Info().title("E-commerce Product Prediction API")
                        .description("Intelligent E-commerce API with vector search, analytics, and prediction capabilities")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("API Support")
                                .email("walterchen.ca@gmail.com"))
                        .license(new License().name("MIT License").url("https://opensource.org/licenses/MIT")))
                .servers(Arrays.asList(
                        new Server().url("http://localhost:" + serverPort).description("Development Server"),
                        new Server().url("https://api.example.com").description("Production Server")
                ))
                .components(new Components()
                        .addSecuritySchemes("api_key", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("api-key")));

        // Apply MCP customizer
        mcpOpenApiCustomizer.customise(openAPI);

        return openAPI;
    }
} 