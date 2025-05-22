package com.example.productapi.service.impl;

import com.example.productapi.dto.SimilarProductSearchRequest;
import com.example.productapi.model.Product;
import com.example.productapi.service.ProductService;
import com.example.productapi.service.ProductSimilarityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductSimilarityServiceImpl implements ProductSimilarityService {

    private final ProductService productService;
    
    @Autowired
    public ProductSimilarityServiceImpl(ProductService productService) {
        this.productService = productService;
    }
    
    /**
     * 查找相似商品
     */
    @Override
    public Map<String, Object> findSimilarProducts(Map<String, Object> request) {
        // 创建请求对象
        SimilarProductSearchRequest searchRequest = new SimilarProductSearchRequest();
        
        // 设置描述文本（如果有）
        if (request.containsKey("description")) {
            searchRequest.setDescription(request.get("description").toString());
        }
        
        // 设置商品ID（如果有）
        if (request.containsKey("productId")) {
            searchRequest.setProductId(request.get("productId").toString());
        }
        
        // 获取结果数量限制（如果有）
        int limit = 10; // 默认限制为10个结果
        if (request.containsKey("limit")) {
            try {
                limit = Integer.parseInt(request.get("limit").toString());
            } catch (NumberFormatException e) {
                // 使用默认值
            }
        }
        
        // 调用产品服务查找相似产品
        List<Product> similarProducts = productService.findSimilarProducts(searchRequest);
        
        // 应用限制（如果需要）
        if (similarProducts.size() > limit) {
            similarProducts = similarProducts.subList(0, limit);
        }
        
        // 构建响应
        Map<String, Object> result = new HashMap<>();
        result.put("products", similarProducts);
        result.put("count", similarProducts.size());
        
        return result;
    }
} 