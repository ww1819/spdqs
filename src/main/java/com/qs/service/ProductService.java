package com.qs.service;

import com.qs.entity.Product;
import com.qs.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> listEnabled() {
        return productRepository.findByEnabledTrueOrderBySortOrderAscNameAsc();
    }

    public Product getById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("产品不存在"));
    }
}
