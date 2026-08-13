package com.qs.repository;

import com.qs.entity.DeliveryNodeChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryNodeChangeLogRepository extends JpaRepository<DeliveryNodeChangeLog, String> {

    List<DeliveryNodeChangeLog> findByNodeIdOrderByChangeTimeDesc(String nodeId);

    void deleteByNodeId(String nodeId);
}
