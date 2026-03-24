package com.founderlink.userservice.repository;

import com.founderlink.userservice.entities.Investor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestorRepository extends JpaRepository<Investor, Long> {

    Page<Investor> findByPreferredIndustries(String industry, Pageable pageable);
}
