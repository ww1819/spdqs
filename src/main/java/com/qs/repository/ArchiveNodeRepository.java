package com.qs.repository;

import com.qs.entity.ArchiveNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArchiveNodeRepository extends JpaRepository<ArchiveNode, String> {

    List<ArchiveNode> findByArchiveIdOrderByStartDateAscSortOrderAsc(String archiveId);

    List<ArchiveNode> findByStage(String stage);

    void deleteByArchiveId(String archiveId);

    long countByArchiveId(String archiveId);

    long countByStage(String stage);
}
