package com.founderlink.userservice.repository;

import com.founderlink.userservice.entities.Founder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FounderRepository extends JpaRepository<Founder, Long> {

    Page<Founder> findByIndustry(String industry, Pageable pageable);
}
