package com.founderlink.userservice.repository;

import com.founderlink.userservice.entities.UserProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

  Optional<UserProfile> findByUserId(Long userId);
}
