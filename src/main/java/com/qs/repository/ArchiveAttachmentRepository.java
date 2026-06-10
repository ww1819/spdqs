package com.qs.repository;

import com.qs.entity.ArchiveAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArchiveAttachmentRepository extends JpaRepository<ArchiveAttachment, String> {

    @Query("SELECT a FROM ArchiveAttachment a WHERE a.archive.id = :archiveId ORDER BY a.createTime DESC")
    List<ArchiveAttachment> findByArchiveIdOrderByCreateTimeDesc(@Param("archiveId") String archiveId);

    @Modifying
    @Query("DELETE FROM ArchiveAttachment a WHERE a.archive.id = :archiveId")
    void deleteByArchiveId(@Param("archiveId") String archiveId);

    @Query("SELECT a FROM ArchiveAttachment a JOIN FETCH a.archive WHERE a.id = :id")
    Optional<ArchiveAttachment> findByIdWithArchive(@Param("id") String id);
}
