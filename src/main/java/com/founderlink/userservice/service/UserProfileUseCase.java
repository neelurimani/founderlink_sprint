package com.founderlink.userservice.service;

import com.founderlink.userservice.dto.CoFounderProfileUpsertRequest;
import com.founderlink.userservice.dto.FounderProfileUpsertRequest;
import com.founderlink.userservice.dto.InvestorProfileUpsertRequest;
import com.founderlink.userservice.dto.UserProfileResponse;
import com.founderlink.userservice.dto.UserProfileUpsertRequest;
import org.springframework.data.domain.Page;

public interface UserProfileUseCase {

    UserProfileResponse create(UserProfileUpsertRequest request);

    UserProfileResponse createFounder(FounderProfileUpsertRequest request);

    UserProfileResponse createCoFounder(CoFounderProfileUpsertRequest request);

    UserProfileResponse createInvestor(InvestorProfileUpsertRequest request);

    UserProfileResponse getByUserId(Long userId);

    Page<UserProfileResponse> list(int page, int size);

    UserProfileResponse update(Long userId, UserProfileUpsertRequest request);

    UserProfileResponse updateFounder(Long userId, FounderProfileUpsertRequest request);

    UserProfileResponse updateCoFounder(Long userId, CoFounderProfileUpsertRequest request);

    UserProfileResponse updateInvestor(Long userId, InvestorProfileUpsertRequest request);
}
