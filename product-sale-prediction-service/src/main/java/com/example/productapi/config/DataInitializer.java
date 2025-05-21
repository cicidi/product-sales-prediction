//package com.example.productapi.config;
//
//import com.example.productapi.model.Product;
//import com.example.productapi.repository.ProductRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.math.BigDecimal;
//import java.util.Arrays;
//
//@Configuration
//public class DataInitializer {
//
//    @Bean
//    public CommandLineRunner initData(ProductRepository productRepository) {
//        return args -> {
//            // Create sample products
//            Product product1 = new Product(null, "Laptop", "High-performance laptop", new BigDecimal("1299.99"), "Electronics", 10);
//            Product product2 = new Product(null, "Smartphone", "Latest smartphone model", new BigDecimal("899.99"), "Electronics", 15);
//            Product product3 = new Product(null, "Headphones", "Wireless noise-cancelling headphones", new BigDecimal("249.99"), "Audio", 20);
//            Product product4 = new Product(null, "Coffee Maker", "Automatic coffee machine", new BigDecimal("89.99"), "Home Appliances", 8);
//            Product product5 = new Product(null, "Running Shoes", "Comfortable athletic shoes", new BigDecimal("79.99"), "Footwear", 25);
//
//            // Save products to database
//            productRepository.saveAll(Arrays.asList(product1, product2, product3, product4, product5));
//
//            System.out.println("Sample products have been initialized!");
//        };
//    }
//}