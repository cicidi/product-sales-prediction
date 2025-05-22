package com.example.productapi.service;

import com.example.productapi.dto.SimilarProductSearchRequest;
import com.example.productapi.model.Product;
import com.example.productapi.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/*
 * Service interface for product operations
 */
public interface ProductService {

    /**
     * Get all products, optionally filtered by category and/or seller ID
     */
    List<Product> getAllProducts(String category, String sellerId);
    
    /**
     * Get product by ID
     */
    Optional<Product> getProductById(String id);
    
    /**
     * Find similar products based on product ID or description
     */
    List<Product> findSimilarProducts(SimilarProductSearchRequest request);

    /**
     * 创建新产品
     * 
     * @param productData 产品数据字段映射:
     *                    - name: 产品名称
     *                    - category: 产品类别
     *                    - brand: 产品品牌
     *                    - price: 产品价格
     *                    - description: 产品描述(可选)
     *                    - sellerId: 卖家ID
     * @return 创建的产品
     */
    Product createProduct(Map<String, Object> productData);
    
    /**
     * 更新产品
     * 
     * @param id 要更新的产品ID
     * @param productData 需要更新的产品字段映射
     * @return 更新后的产品，如果产品不存在则返回null
     */
    Product updateProduct(String id, Map<String, Object> productData);
} 