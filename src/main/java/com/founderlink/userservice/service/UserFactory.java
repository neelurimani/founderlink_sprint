package com.founderlink.userservice.service;

import com.founderlink.userservice.entities.Admin;
import com.founderlink.userservice.entities.CoFounder;
import com.founderlink.userservice.entities.Founder;
import com.founderlink.userservice.entities.Investor;
import com.founderlink.userservice.entities.User;
import com.founderlink.userservice.entities.UserRole;
import org.springframework.stereotype.Component;

@Component
public class UserFactory {

    public User create(UserRole role, String name, String email) {
        User user = switch (role) {
            case ROLE_FOUNDER -> new Founder();
            case ROLE_INVESTOR -> new Investor();
            case ROLE_COFUNDER -> new CoFounder();
            case ROLE_ADMIN -> new Admin();
        };
        user.setRole(role);
        user.setName(name);
        user.setEmail(email);
        return user;
    }
}
