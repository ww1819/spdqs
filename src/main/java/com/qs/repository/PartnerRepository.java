package com.qs.repository;

import com.qs.entity.Partner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartnerRepository extends JpaRepository<Partner, String> {

    List<Partner> findAllByOrderByNameAsc();
}
