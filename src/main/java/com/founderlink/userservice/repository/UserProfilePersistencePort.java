package com.founderlink.userservice.repository;

import com.founderlink.userservice.entities.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserProfilePersistencePort {

    boolean existsByEmailIgnoreCase(String email);

    UserProfile save(UserProfile profile);

    Optional<UserProfile> findByUserId(Long userId);

    Page<UserProfile> findAll(Pageable pageable);
}
