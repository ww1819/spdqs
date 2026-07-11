package com.qs.repository;

import com.qs.entity.ArchiveNodeStageDef;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArchiveNodeStageDefRepository extends JpaRepository<ArchiveNodeStageDef, String> {

    List<ArchiveNodeStageDef> findByDeletedFalseOrderBySortOrderAscCreateTimeAsc();

    Optional<ArchiveNodeStageDef> findByNameAndDeletedFalse(String name);

    long countByDeletedFalse();
}
