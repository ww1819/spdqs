package com.qs.repository;

import com.qs.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryRepository extends JpaRepository<Delivery, String> {

    List<Delivery> findAllByOrderByCreateTimeDesc();

    List<Delivery> findByCustomerIdOrderByCreateTimeDesc(String customerId);

    long countByProductId(String productId);
}
