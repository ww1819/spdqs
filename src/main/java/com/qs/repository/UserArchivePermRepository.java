package com.qs.repository;

import com.qs.entity.UserArchivePerm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserArchivePermRepository extends JpaRepository<UserArchivePerm, String> {

    List<UserArchivePerm> findByUserId(String userId);

    void deleteByUserId(String userId);

    boolean existsByUserIdAndArchiveId(String userId, String archiveId);

    long countByUserId(String userId);
}
