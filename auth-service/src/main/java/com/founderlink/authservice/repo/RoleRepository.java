package com.founderlink.authservice.repo;

import com.founderlink.authservice.entity.RoleEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
  Optional<RoleEntity> findByNameIgnoreCase(String name);
}
