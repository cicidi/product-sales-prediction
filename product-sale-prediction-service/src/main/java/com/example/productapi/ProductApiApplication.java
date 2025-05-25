package com.example.productapi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
    info = @Info(
        title = " QuickBooks Commerce System Service API",
        version = "1.0",
        description = "AI powered E-commerce System.\n"
            + "This API provides API and MCP tool for checking recent top N sales and predicate future sales .\n",
        contact = @Contact(name = "Author", email = "walterchen.ca@gmail.com"),
        license = @License(name = "MIT License", url = "https://opensource.org/licenses/MIT")
    ),
    servers = {
        @Server(url = "/", description = "http://localhost:8080"),
    }
)
public class ProductApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(ProductApiApplication.class, args);
  }
} 