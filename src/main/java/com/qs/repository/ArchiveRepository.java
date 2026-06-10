package com.qs.repository;

import com.qs.entity.Archive;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArchiveRepository extends JpaRepository<Archive, String> {

    List<Archive> findAllByOrderByCreateTimeDesc();
}
