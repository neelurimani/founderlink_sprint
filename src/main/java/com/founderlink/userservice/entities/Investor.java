package com.founderlink.userservice.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@DiscriminatorValue("INVESTOR")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Investor extends User {

    private Double investmentBudget;

    private String preferredIndustries;
}
