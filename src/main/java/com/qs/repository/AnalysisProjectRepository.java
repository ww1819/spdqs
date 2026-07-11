package com.qs.repository;

import com.qs.entity.AnalysisProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisProjectRepository extends JpaRepository<AnalysisProject, String> {

    List<AnalysisProject> findAllByOrderByCreateTimeDesc();
}
