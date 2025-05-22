package com.example.productapi.service;

/**
 * Service for loading data from CSV files
 */
public interface CSVLoaderService {
    
    /**
     * Load data from CSV files into the database
     * This is typically called when the application starts
     */
    void loadDataFromCSV();
} 