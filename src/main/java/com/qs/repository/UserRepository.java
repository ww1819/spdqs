package com.qs.repository;

import com.qs.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUsername(String username);

    List<User> findAllByOrderByCreateTimeDesc();

    @Query("SELECT COUNT(u) FROM User u WHERE u.username = :username")
    long countByUsername(@Param("username") String username);
}
