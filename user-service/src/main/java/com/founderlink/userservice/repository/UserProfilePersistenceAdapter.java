package com.founderlink.userservice.repository;

import com.founderlink.userservice.entities.UserProfile;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class UserProfilePersistenceAdapter implements UserProfilePersistencePort {

  private final UserRepository userRepository;
  private final UserProfileRepository userProfileRepository;

  public UserProfilePersistenceAdapter(
      UserRepository userRepository, UserProfileRepository userProfileRepository) {
    this.userRepository = userRepository;
    this.userProfileRepository = userProfileRepository;
  }

  @Override
  public boolean existsByEmailIgnoreCase(String email) {
    return userRepository.existsByEmailIgnoreCase(email);
  }

  @Override
  public UserProfile save(UserProfile profile) {
    return userProfileRepository.save(profile);
  }

  @Override
  public Optional<UserProfile> findByUserId(Long userId) {
    return userProfileRepository.findByUserId(userId);
  }

  @Override
  public Page<UserProfile> findAll(Pageable pageable) {
    return userProfileRepository.findAll(pageable);
  }
}
