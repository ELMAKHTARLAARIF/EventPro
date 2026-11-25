package com.eventpro.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.*;

@Entity
@Table(name = "organisateurs")
@Getter @Setter @NoArgsConstructor
public class Organisateur extends User {

    @OneToMany(mappedBy = "organisateur")
    private List<Evenement> evenements = new ArrayList<>();

    @OneToMany(mappedBy = "organisateur")
    private List<Promotion> promotions = new ArrayList<>();
}