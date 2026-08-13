package com.qs.repository;

import com.qs.entity.DeliveryNodeMemo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryNodeMemoRepository extends JpaRepository<DeliveryNodeMemo, String> {

    List<DeliveryNodeMemo> findByNodeIdOrderByCreateTimeDesc(String nodeId);

    void deleteByNodeId(String nodeId);

    long countByNodeId(String nodeId);
}
