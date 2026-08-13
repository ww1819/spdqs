package com.qs.repository;

import com.qs.entity.UserDeliveryPerm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserDeliveryPermRepository extends JpaRepository<UserDeliveryPerm, String> {

    List<UserDeliveryPerm> findByUserId(String userId);

    void deleteByUserId(String userId);

    boolean existsByUserIdAndDeliveryId(String userId, String deliveryId);
}
