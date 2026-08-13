package com.qs.repository;

import com.qs.entity.DeliveryAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeliveryAttachmentRepository extends JpaRepository<DeliveryAttachment, String> {

    @Query("SELECT a FROM DeliveryAttachment a WHERE a.delivery.id = :deliveryId ORDER BY a.createTime DESC")
    List<DeliveryAttachment> findByDeliveryIdOrderByCreateTimeDesc(@Param("deliveryId") String deliveryId);

    @Modifying
    @Query("DELETE FROM DeliveryAttachment a WHERE a.delivery.id = :deliveryId")
    void deleteByDeliveryId(@Param("deliveryId") String deliveryId);

    @Query("SELECT a FROM DeliveryAttachment a JOIN FETCH a.delivery WHERE a.id = :id")
    Optional<DeliveryAttachment> findByIdWithDelivery(@Param("id") String id);
}
