package com.example.productapi.model;

import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "product")
public class Product {

    @Id
    private String id;
    
    private String name;
    private String category;
    private String brand;
    private Double price;
    
    @Column(name = "create_timestamp")
    private LocalDateTime createTimestamp;
    
    @Lob
    @Column(length = 5000)
    private String description;
    
    @Transient
    private List<Float> descriptionVector;

}