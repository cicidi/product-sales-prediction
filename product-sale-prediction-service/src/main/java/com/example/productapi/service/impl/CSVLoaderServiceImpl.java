package com.example.productapi.service.impl;

import com.example.productapi.model.Inventory;
import com.example.productapi.model.Order;
import com.example.productapi.model.Product;
import com.example.productapi.repository.InventoryRepository;
import com.example.productapi.repository.OrderRepository;
import com.example.productapi.repository.ProductRepository;
import com.example.productapi.service.CSVLoaderService;
import com.openai.services.blocking.EmbeddingService;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class CSVLoaderServiceImpl implements CSVLoaderService {

  @Value("${csv.product-file:final_sample_products.csv}")
  private String productCsvFile;

  @Value("${csv.sales-file:sales_2023_2025_realistic.csv}")
  private String salesCsvFile;

  private final ProductRepository productRepository;
  private final OrderRepository orderRepository;
  private final InventoryRepository inventoryRepository;

  public CSVLoaderServiceImpl(
      ProductRepository productRepository,
      OrderRepository orderRepository,
      InventoryRepository inventoryRepository) {
    this.productRepository = productRepository;
    this.orderRepository = orderRepository;
    this.inventoryRepository = inventoryRepository;
  }

  @Override
  public void loadDataFromCSV() {
    init();
  }

  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void init() {
    try {
      // 清空现有数据
//      orderRepository.deleteAll();
//      inventoryRepository.deleteAll();
//      productRepository.deleteAll();

      // 加载产品和销售数据
//      loadProducts();
//      loadSales();
      log.info("Data initialization complete.");
    } catch (Exception e) {
      log.error("Error initializing data: {}", e.getMessage(), e);
    }
  }

  private void loadProducts() throws IOException, CsvValidationException {
    log.info("Loading products from resources/{}", productCsvFile);
    List<Product> products = new ArrayList<>();

    try {
      ClassPathResource resource = new ClassPathResource(productCsvFile);
      try (CSVReader reader = new CSVReader(new InputStreamReader(resource.getInputStream()))) {
        // Skip header
        String[] header = reader.readNext();
        String[] line;

        while ((line = reader.readNext()) != null) {
          try {
            Product product = Product.builder()
                .id(line[0])
                .name(line[1])
                .category(line[2])
                .brand(line[3])
                .price(Double.parseDouble(line[4]))
                .createTimestamp(LocalDateTime.parse(line[5]))
                .description(line[6])
                .build();

            products.add(product);
          } catch (Exception e) {
            log.error("Error parsing product line: {}", String.join(",", line), e);
          }
        }
      }
    } catch (IOException e) {
      log.error("Error reading product CSV from resources: {}", e.getMessage());
      throw e;
    }

    log.info("Saving {} products to database", products.size());
    productRepository.saveAll(products);
  }

  private void loadSales() throws IOException, CsvValidationException {
    log.info("Loading orders from resources/{}", salesCsvFile);

    List<Order> orders = new ArrayList<>();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    Set<String> processedProductSellers = new HashSet<>();
    List<Inventory> inventoryItems = new ArrayList<>();

    try {
      ClassPathResource resource = new ClassPathResource(salesCsvFile);
      try (CSVReader reader = new CSVReader(new InputStreamReader(resource.getInputStream()))) {
        // Skip header
        String[] header = reader.readNext();
        String[] line;

        while ((line = reader.readNext()) != null) {
          try {
            String productId = line[1];
            String sellerId = line[3];
            ; // 使用固定的卖家ID
            LocalDateTime timestamp = LocalDateTime.parse(line[7], formatter);

            Order order = Order.builder()
                .orderId(line[0])
                .productId(productId)
                .buyerId(line[2])
                .sellerId(sellerId)
                .unitPrice(Double.parseDouble(line[4]))
                .quantity(Integer.parseInt(line[5]))
                .totalPrice(Double.parseDouble(line[6]))
                .timestamp(timestamp)
                .build();

            orders.add(order);

            // Create inventory record if not already processed
            String key = productId + "_" + sellerId;
            if (!processedProductSellers.contains(key)) {
              Inventory inventory = Inventory.builder()
                  .productId(productId)
                  .sellerId(sellerId)
                  .createTimeStamp(timestamp)
                  .quantity(100)  // Default quantity
                  .build();
              inventoryItems.add(inventory);
              processedProductSellers.add(key);
            }
          } catch (Exception e) {
            log.error("Error parsing order line: {}", String.join(",", line), e);
          }
        }
      }
    } catch (IOException e) {
      log.error("Error reading sales CSV from resources: {}", e.getMessage());
      throw e;
    }

    log.info("Saving {} orders to database", orders.size());
    orderRepository.saveAll(orders);

    log.info("Saving {} inventory items to database", inventoryItems.size());
    inventoryRepository.saveAll(inventoryItems);
  }
}