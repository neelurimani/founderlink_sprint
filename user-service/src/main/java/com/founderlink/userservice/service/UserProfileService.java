package com.founderlink.userservice.service;

import com.founderlink.userservice.dto.CoFounderProfileUpsertRequest;
import com.founderlink.userservice.dto.FounderProfileUpsertRequest;
import com.founderlink.userservice.dto.InvestorProfileUpsertRequest;
import com.founderlink.userservice.dto.UserProfileResponse;
import com.founderlink.userservice.dto.UserProfileUpsertRequest;
import com.founderlink.userservice.entities.CoFounder;
import com.founderlink.userservice.entities.Founder;
import com.founderlink.userservice.entities.Investor;
import com.founderlink.userservice.entities.User;
import com.founderlink.userservice.entities.UserProfile;
import com.founderlink.userservice.entities.UserRole;
import com.founderlink.userservice.exception.DuplicateUserProfileException;
import com.founderlink.userservice.exception.ForbiddenUpdateException;
import com.founderlink.userservice.exception.UnauthorizedException;
import com.founderlink.userservice.exception.UserProfileNotFoundException;
import com.founderlink.userservice.repository.UserProfilePersistencePort;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService implements UserProfileUseCase {

  private final UserProfilePersistencePort persistencePort;
  private final UserFactory userFactory;
  private final UserProfileMapper userProfileMapper;

  public UserProfileService(
      UserProfilePersistencePort persistencePort,
      UserFactory userFactory,
      UserProfileMapper userProfileMapper) {
    this.persistencePort = persistencePort;
    this.userFactory = userFactory;
    this.userProfileMapper = userProfileMapper;
  }

  @Override
  @Transactional
  @CacheEvict(cacheNames = "userProfiles", allEntries = true)
  public UserProfileResponse create(UserProfileUpsertRequest request) {
    if (isAdmin()) {
      throw new ForbiddenUpdateException("ADMIN cannot create user profiles");
    }
    // Security rule:
    // A caller can create only ONE profile in user-service, and the created profile id must
    // match the authenticated JWT userId (principal name in this service).
    Long callerUserId = requireCallerUserId();

    if (persistencePort.findByUserId(callerUserId).isPresent()) {
      throw new DuplicateUserProfileException(
          "A user profile already exists for user id " + callerUserId);
    }

    String normalizedEmail = normalizeEmail(request.email());
    if (persistencePort.existsByEmailIgnoreCase(normalizedEmail)) {
      throw new DuplicateUserProfileException(
          "A user profile already exists for email " + request.email());
    }

    UserRole role = UserRole.fromValue(request.role());
    User user = userFactory.create(role, request.name().trim(), normalizedEmail);
    // Persist with the authenticated JWT user id as the primary key so subsequent
    // self-authorization
    // (updates/list ownership) is stable and deterministic.
    user.setId(callerUserId);
    applyRoleSpecificCreateData(user, request);
    UserProfile profile = userProfileMapper.toNewEntity(user, request);
    UserProfile saved = persistencePort.save(profile);

    // Enforce caller -> profile id alignment for later self-authorization checks.
    // In this service, API "id" is the underlying `User.id` (not `UserProfile.id`).
    Long createdUserId = saved.getUser() == null ? null : saved.getUser().getId();
    if (createdUserId == null || !createdUserId.equals(callerUserId)) {
      throw new ForbiddenUpdateException("JWT user id must match the created profile id");
    }

    return userProfileMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public UserProfileResponse createFounder(FounderProfileUpsertRequest request) {
    return create(withRole(request, "founder"));
  }

  @Override
  @Transactional
  public UserProfileResponse createCoFounder(CoFounderProfileUpsertRequest request) {
    return create(withRole(request, "cofounder"));
  }

  @Override
  @Transactional
  public UserProfileResponse createInvestor(InvestorProfileUpsertRequest request) {
    return create(withRole(request, "investor"));
  }

  @Override
  @Transactional(readOnly = true)
  @Cacheable(cacheNames = "userProfiles", key = "#userId")
  public UserProfileResponse getByUserId(Long userId) {
    Long callerUserId = requireCallerUserId();
    if (!isAdmin() && !callerUserId.equals(userId)) {
      throw new ForbiddenUpdateException("You can only view your own profile");
    }
    return userProfileMapper.toResponse(findProfile(userId));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<UserProfileResponse> list(int page, int size) {
    PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Order.asc("user.id")));
    return persistencePort.findAll(pageable).map(userProfileMapper::toResponse);
  }

  @Override
  @Transactional
  @CacheEvict(cacheNames = "userProfiles", key = "#userId")
  public UserProfileResponse update(Long userId, UserProfileUpsertRequest request) {
    assertCallerIsSelf(userId);
    UserProfile profile = findProfile(userId);
    User user = profile.getUser();

    String normalizedEmail = normalizeEmail(request.email());
    if (!user.getEmail().equalsIgnoreCase(normalizedEmail)
        && persistencePort.existsByEmailIgnoreCase(normalizedEmail)) {
      throw new DuplicateUserProfileException(
          "A user profile already exists for email " + request.email());
    }

    UserRole role = UserRole.fromValue(request.role());
    if (user.getRole() != role) {
      throw new IllegalArgumentException("Role changes are not supported for existing users");
    }

    user.setName(request.name().trim());
    user.setEmail(normalizedEmail);
    userProfileMapper.applyUpdate(profile, request);
    applyRoleSpecificUpdateData(user, request);

    return userProfileMapper.toResponse(profile);
  }

  @Override
  @Transactional
  public UserProfileResponse updateFounder(Long userId, FounderProfileUpsertRequest request) {
    return update(userId, withRole(request, "founder"));
  }

  @Override
  @Transactional
  public UserProfileResponse updateCoFounder(Long userId, CoFounderProfileUpsertRequest request) {
    return update(userId, withRole(request, "cofounder"));
  }

  @Override
  @Transactional
  public UserProfileResponse updateInvestor(Long userId, InvestorProfileUpsertRequest request) {
    return update(userId, withRole(request, "investor"));
  }

  private UserProfile findProfile(Long userId) {
    return persistencePort
        .findByUserId(userId)
        .orElseThrow(
            () -> new UserProfileNotFoundException("User profile not found for id " + userId));
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase();
  }

  private UserProfileUpsertRequest withRole(FounderProfileUpsertRequest request, String role) {
    return new UserProfileUpsertRequest(
        request.name(),
        request.email(),
        role,
        request.skills(),
        request.experience(),
        request.bio(),
        request.portfolioLinks(),
        request.startupName(),
        request.industry(),
        request.fundingGoal(),
        null,
        null,
        null,
        null);
  }

  private UserProfileUpsertRequest withRole(CoFounderProfileUpsertRequest request, String role) {
    return new UserProfileUpsertRequest(
        request.name(),
        request.email(),
        role,
        request.skills(),
        request.experience(),
        request.bio(),
        request.portfolioLinks(),
        null,
        null,
        null,
        request.expertise(),
        request.coFounderExperience(),
        null,
        null);
  }

  private UserProfileUpsertRequest withRole(InvestorProfileUpsertRequest request, String role) {
    return new UserProfileUpsertRequest(
        request.name(),
        request.email(),
        role,
        request.skills(),
        request.experience(),
        request.bio(),
        request.portfolioLinks(),
        null,
        null,
        null,
        null,
        null,
        request.investmentBudget(),
        request.preferredIndustries());
  }

  private void applyRoleSpecificCreateData(User user, UserProfileUpsertRequest request) {
    applyRoleSpecificUpdateData(user, request);
  }

  private void applyRoleSpecificUpdateData(User user, UserProfileUpsertRequest request) {
    if (user instanceof Founder founder) {
      if (request.startupName() == null
          && request.industry() == null
          && request.fundingGoal() == null) {
        return;
      }
      founder.setStartupName(requireNonBlank(request.startupName(), "startupName"));
      founder.setIndustry(requireNonBlank(request.industry(), "industry"));
      founder.setFundingGoal(requirePositive(request.fundingGoal(), "fundingGoal"));
      return;
    }

    if (user instanceof CoFounder coFounder) {
      if (request.expertise() == null && request.coFounderExperience() == null) {
        return;
      }
      coFounder.setExpertise(requireNonBlank(request.expertise(), "expertise"));
      coFounder.setExperience(request.coFounderExperience());
      return;
    }

    if (user instanceof Investor investor) {
      if (request.investmentBudget() == null && request.preferredIndustries() == null) {
        return;
      }
      investor.setInvestmentBudget(requirePositive(request.investmentBudget(), "investmentBudget"));
      investor.setPreferredIndustries(
          requireNonBlank(request.preferredIndustries(), "preferredIndustries"));
    }
  }

  private String requireNonBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value.trim();
  }

  private Double requirePositive(Double value, String fieldName) {
    if (value == null || value <= 0) {
      throw new IllegalArgumentException(fieldName + " must be positive");
    }
    return value;
  }

  private void assertCallerIsSelf(Long targetUserId) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || authentication.getName() == null
        || authentication.getName().isBlank()) {
      throw new UnauthorizedException("Missing authentication");
    }
    Long callerUserId;
    try {
      callerUserId = Long.valueOf(authentication.getName());
    } catch (NumberFormatException ex) {
      throw new UnauthorizedException("Invalid authenticated user id");
    }

    if (!targetUserId.equals(callerUserId)) {
      throw new ForbiddenUpdateException("You can only update your own profile");
    }
  }

  private Long requireCallerUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || authentication.getName() == null
        || authentication.getName().isBlank()) {
      throw new UnauthorizedException("Missing authentication");
    }
    try {
      return Long.valueOf(authentication.getName());
    } catch (NumberFormatException ex) {
      throw new UnauthorizedException("Invalid authenticated user id");
    }
  }

  private boolean isAdmin() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getAuthorities() == null) {
      return false;
    }
    return authentication.getAuthorities().stream()
        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
  }
}
