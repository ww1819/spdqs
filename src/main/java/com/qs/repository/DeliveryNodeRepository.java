package com.qs.repository;

import com.qs.entity.DeliveryNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryNodeRepository extends JpaRepository<DeliveryNode, String> {

    List<DeliveryNode> findByDeliveryIdOrderByStartDateAscSortOrderAsc(String deliveryId);

    List<DeliveryNode> findByStage(String stage);

    void deleteByDeliveryId(String deliveryId);

    long countByDeliveryId(String deliveryId);

    long countByStage(String stage);
}
