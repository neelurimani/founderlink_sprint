package com.founderlink.userservice.repository;

import com.founderlink.userservice.entities.CoFounder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoFounderRepository extends JpaRepository<CoFounder, Long> {

  Page<CoFounder> findByExpertise(String expertise, Pageable pageable);
}
