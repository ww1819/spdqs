package com.qs.repository;

import com.qs.entity.FlowNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FlowNodeRepository extends JpaRepository<FlowNode, String> {

    List<FlowNode> findByProjectIdOrderBySortOrderAsc(String projectId);

    List<FlowNode> findByParentIdOrderBySortOrderAsc(String parentId);

    Optional<FlowNode> findByProjectIdAndParentIdIsNull(String projectId);

    void deleteByProjectId(String projectId);

    long countByProjectId(String projectId);
}
