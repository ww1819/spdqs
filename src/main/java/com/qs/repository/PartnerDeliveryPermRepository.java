package com.qs.repository;

import com.qs.entity.PartnerDeliveryPerm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartnerDeliveryPermRepository extends JpaRepository<PartnerDeliveryPerm, String> {

    List<PartnerDeliveryPerm> findByPartnerId(String partnerId);

    void deleteByPartnerId(String partnerId);

    boolean existsByPartnerIdAndDeliveryId(String partnerId, String deliveryId);
}
