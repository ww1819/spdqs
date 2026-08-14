package com.qs.service;

import com.qs.entity.Product;
import com.qs.repository.DeliveryRepository;
import com.qs.repository.ProductRepository;
import com.qs.util.PinyinCodeUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final DeliveryRepository deliveryRepository;

    public ProductService(ProductRepository productRepository, DeliveryRepository deliveryRepository) {
        this.productRepository = productRepository;
        this.deliveryRepository = deliveryRepository;
    }

    public List<Product> listAll() {
        return productRepository.findAllByOrderBySortOrderAscNameAsc();
    }

    public List<Product> listEnabled() {
        return productRepository.findByEnabledTrueOrderBySortOrderAscNameAsc();
    }

    public List<Product> search(String keyword) {
        String kw = normalize(keyword);
        if (kw == null) {
            return listAll();
        }
        return listAll().stream()
                .filter(p -> contains(p.getName(), kw)
                        || contains(p.getNamePy(), kw)
                        || contains(p.getCode(), kw)
                        || contains(p.getRemark(), kw))
                .toList();
    }

    public Product getById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("产品不存在"));
    }

    public long countDeliveries(String productId) {
        return deliveryRepository.countByProductId(productId);
    }

    @Transactional
    public Product save(Product product) {
        if (product.getName() == null || product.getName().isBlank()) {
            throw new IllegalArgumentException("产品名称不能为空");
        }
        String name = product.getName().trim();
        product.setName(name);
        String code = product.getCode() == null || product.getCode().isBlank()
                ? PinyinCodeUtil.toJianpin(name)
                : product.getCode().trim();
        if (code.isBlank()) {
            throw new IllegalArgumentException("产品编码不能为空");
        }
        productRepository.findByCode(code).ifPresent(existing -> {
            if (product.getId() == null || !existing.getId().equals(product.getId())) {
                throw new IllegalArgumentException("产品编码已存在：" + code);
            }
        });
        product.setCode(code);
        if (product.getId() == null || product.getId().isBlank()) {
            if (product.getSortOrder() <= 0) {
                product.setSortOrder(nextSortOrder());
            }
        }
        return productRepository.save(product);
    }

    @Transactional
    public void delete(String id) {
        if (countDeliveries(id) > 0) {
            throw new IllegalArgumentException("该产品下仍有产品交付，请先删除或转移交付后再删");
        }
        productRepository.deleteById(id);
    }

    private int nextSortOrder() {
        return listAll().stream()
                .mapToInt(Product::getSortOrder)
                .max()
                .orElse(0) + 10;
    }

    private String normalize(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }

    private boolean contains(String text, String kw) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(kw);
    }
}
