package com.eventpro.entity;

import com.eventpro.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "organisateurs")
@Getter @Setter @NoArgsConstructor
public class Organisateur extends User {

    private String nomOrganisation;

    @OneToMany(mappedBy = "organisateur")
    private List<Evenement> evenements = new ArrayList<>();

    @OneToMany(mappedBy = "organisateur")
    private List<Promotion> promotions = new ArrayList<>();
}
