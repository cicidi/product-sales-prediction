package com.example.productapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchResponse {
    
    private List<Double> predictions;
    private Integer count;
    private String status = "success";
} 