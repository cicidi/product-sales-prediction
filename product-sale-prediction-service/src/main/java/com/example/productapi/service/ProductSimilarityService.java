package com.example.productapi.service;

import java.util.Map;

/**
 * 商品相似度搜索服务
 */
public interface ProductSimilarityService {
    
    /**
     * 查找相似商品
     * 
     * @param request 包含搜索参数:
     *                - description: 商品描述文本，用于查找与此描述相似的商品
     *                - productId: 商品ID，用于查找与此商品相似的其他商品
     *                - limit: 返回结果的数量限制
     * @return 包含结果的Map:
     *         - products: 相似商品列表
     *         - count: 返回的商品数量
     */
    Map<String, Object> findSimilarProducts(Map<String, Object> request);
} 