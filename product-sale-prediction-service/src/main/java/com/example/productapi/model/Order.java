package com.example.productapi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "orders")
public class Order {

    @Id
    private String orderId;
    
    private String productId;
    private String buyerId;
    private String sellerId;
    
    private Double unitPrice;
    private Integer quantity;
    private Double totalPrice;
    
    @Column(name = "timestamp")
    private LocalDateTime timestamp;
} 