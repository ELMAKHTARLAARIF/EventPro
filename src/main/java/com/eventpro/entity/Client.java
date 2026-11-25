package com.eventpro.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.*;

@Entity
@Table(name = "clients")
@Getter @Setter @NoArgsConstructor
public class Client extends User {
    private String telephone;

@OneToMany(mappedBy = "client")
private List<Reservation> reservations = new ArrayList<>();

}
