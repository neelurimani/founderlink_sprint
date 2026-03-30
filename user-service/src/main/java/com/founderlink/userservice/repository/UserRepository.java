package com.founderlink.userservice.repository;

import com.founderlink.userservice.entities.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  Page<User> findAll(Pageable pageable);

  boolean existsByEmailIgnoreCase(String email);

  Optional<User> findByEmailIgnoreCase(String email);
}
