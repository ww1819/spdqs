package com.qs.repository;

import com.qs.entity.DeliveryNodeAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryNodeAttachmentRepository extends JpaRepository<DeliveryNodeAttachment, String> {

    List<DeliveryNodeAttachment> findByNodeIdOrderByCreateTimeDesc(String nodeId);

    void deleteByNodeId(String nodeId);

    long countByNodeId(String nodeId);
}
