//package com.example.productapi.service.impl;
//
//import com.example.productapi.model.Product;
//import com.example.productapi.repository.ProductRepository;
//import com.example.productapi.service.ProductService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.Optional;
//
//@Service
//public class ProductServiceImpl implements ProductService {
//
//    private final ProductRepository productRepository;
//
//    @Autowired
//    public ProductServiceImpl(ProductRepository productRepository) {
//        this.productRepository = productRepository;
//    }
//
//    @Override
//    public List<Product> getAllProducts() {
//        return productRepository.findAll();
//    }
//
//    @Override
//    public Optional<Product> getProductById(Long id) {
//        return productRepository.findById(id);
//    }
//
//    @Override
//    public Product saveProduct(Product product) {
//        return productRepository.save(product);
//    }
//
//    @Override
//    public void deleteProduct(Long id) {
//        productRepository.deleteById(id);
//    }
//}