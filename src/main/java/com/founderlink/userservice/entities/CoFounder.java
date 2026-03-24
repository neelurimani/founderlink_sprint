package com.founderlink.userservice.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@DiscriminatorValue("COFOUNDER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CoFounder extends User {

    private String expertise;


    private String experience;
}
