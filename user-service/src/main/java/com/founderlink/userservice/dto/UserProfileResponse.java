package com.founderlink.userservice.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "role",
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = FounderProfileResponse.class, name = "FOUNDER"),
  @JsonSubTypes.Type(value = CoFounderProfileResponse.class, name = "COFOUNDER"),
  @JsonSubTypes.Type(value = InvestorProfileResponse.class, name = "INVESTOR"),
  @JsonSubTypes.Type(value = AdminProfileResponse.class, name = "ADMIN")
})
public class UserProfileResponse {
  private Long id;
  private String name;
  private String email;
  private String role;
  private String skills;
  private String experience;
  private String bio;
  private String portfolioLinks;
}
