package com.qs.repository;

import com.qs.entity.FlowNodeChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FlowNodeChangeLogRepository extends JpaRepository<FlowNodeChangeLog, String> {

    List<FlowNodeChangeLog> findByNodeIdOrderByChangeTimeDesc(String nodeId);

    List<FlowNodeChangeLog> findByProjectIdOrderByChangeTimeDesc(String projectId);

    @Query("""
            select c from FlowNodeChangeLog c
            where c.oldTitle = :title or c.newTitle = :title
            """)
    List<FlowNodeChangeLog> findByTitleInvolved(@Param("title") String title);
}
