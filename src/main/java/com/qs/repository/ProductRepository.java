package com.qs.repository;

import com.qs.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String> {

    Optional<Product> findByCode(String code);

    List<Product> findAllByOrderBySortOrderAscNameAsc();

    List<Product> findByEnabledTrueOrderBySortOrderAscNameAsc();
}
