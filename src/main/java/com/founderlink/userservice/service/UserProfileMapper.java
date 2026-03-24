package com.founderlink.userservice.service;

import com.founderlink.userservice.dto.AdminProfileResponse;
import com.founderlink.userservice.dto.CoFounderProfileResponse;
import com.founderlink.userservice.dto.FounderProfileResponse;
import com.founderlink.userservice.dto.InvestorProfileResponse;
import com.founderlink.userservice.dto.UserProfileResponse;
import com.founderlink.userservice.dto.UserProfileUpsertRequest;
import com.founderlink.userservice.entities.CoFounder;
import com.founderlink.userservice.entities.Founder;
import com.founderlink.userservice.entities.Investor;
import com.founderlink.userservice.entities.User;
import com.founderlink.userservice.entities.UserProfile;
import com.founderlink.userservice.entities.UserRole;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper {

    private final ModelMapper modelMapper;

    public UserProfileMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public UserProfile toNewEntity(User user, UserProfileUpsertRequest request) {
        UserProfile profile = modelMapper.map(request, UserProfile.class);
        profile.setPortfolioLink(request.portfolioLinks());
        profile.setUser(user);
        return profile;
    }

    public UserProfileResponse toResponse(UserProfile profile) {
        UserProfileResponse response = buildRoleResponse(profile.getUser());
        response.setId(profile.getUser().getId());
        response.setName(profile.getUser().getName());
        response.setEmail(profile.getUser().getEmail());
        response.setRole(toApiRole(profile.getUser().getRole()));
        response.setSkills(profile.getSkills());
        response.setExperience(profile.getExperience());
        response.setBio(profile.getBio());
        response.setPortfolioLinks(profile.getPortfolioLink());
        return response;
    }

    public void applyUpdate(UserProfile profile, UserProfileUpsertRequest request) {
        profile.setSkills(request.skills());
        profile.setExperience(request.experience());
        profile.setBio(request.bio());
        profile.setPortfolioLink(request.portfolioLinks());
    }

    private String toApiRole(UserRole role) {
        if (role == UserRole.ROLE_COFUNDER) {
            return "COFOUNDER";
        }
        return role.name().replace("ROLE_", "");
    }

    private UserProfileResponse buildRoleResponse(User user) {
        if (user instanceof Founder founder) {
            FounderProfileResponse response = new FounderProfileResponse();
            response.setStartupName(founder.getStartupName());
            response.setIndustry(founder.getIndustry());
            response.setFundingGoal(founder.getFundingGoal());
            return response;
        }
        if (user instanceof CoFounder coFounder) {
            CoFounderProfileResponse response = new CoFounderProfileResponse();
            response.setExpertise(coFounder.getExpertise());
            response.setCoFounderExperience(coFounder.getExperience());
            return response;
        }
        if (user instanceof Investor investor) {
            InvestorProfileResponse response = new InvestorProfileResponse();
            response.setInvestmentBudget(investor.getInvestmentBudget());
            response.setPreferredIndustries(investor.getPreferredIndustries());
            return response;
        }
        return new AdminProfileResponse();
    }
}
