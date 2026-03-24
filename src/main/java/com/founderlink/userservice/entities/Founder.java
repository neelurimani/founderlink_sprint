package com.founderlink.userservice.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@DiscriminatorValue("FOUNDER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Founder extends User {

    private String startupName;

    private String industry;

    private Double fundingGoal;
}
