package com.qs.repository;

import com.qs.entity.UserMenuPerm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserMenuPermRepository extends JpaRepository<UserMenuPerm, String> {

    List<UserMenuPerm> findByUserId(String userId);

    void deleteByUserId(String userId);

    boolean existsByUserIdAndMenuCode(String userId, String menuCode);

    long countByUserId(String userId);
}
