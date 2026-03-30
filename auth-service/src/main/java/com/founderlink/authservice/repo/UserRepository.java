package com.founderlink.authservice.repo;

import com.founderlink.authservice.entity.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<AppUser, Long> {
  boolean existsByEmailIgnoreCase(String email);

  Optional<AppUser> findByEmailIgnoreCase(String email);
}
