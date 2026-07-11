package com.qs.repository;

import com.qs.entity.FlowNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FlowNodeRepository extends JpaRepository<FlowNode, String> {

    List<FlowNode> findByProjectIdAndDeletedFalseOrderBySortOrderAsc(String projectId);

    List<FlowNode> findByParentIdAndDeletedFalseOrderBySortOrderAsc(String parentId);

    List<FlowNode> findByTitle(String title);

    List<FlowNode> findByDeletedFalse();

    Optional<FlowNode> findByProjectIdAndParentIdIsNullAndDeletedFalse(String projectId);

    void deleteByProjectId(String projectId);

    long countByProjectIdAndDeletedFalse(String projectId);
}
