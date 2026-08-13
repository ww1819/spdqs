package com.qs.repository;

import com.qs.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, String> {

    Optional<Customer> findByName(String name);

    List<Customer> findAllByOrderByNameAsc();
}
