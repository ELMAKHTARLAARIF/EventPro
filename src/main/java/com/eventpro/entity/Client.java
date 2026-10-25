package com.eventpro.entity;

import com.eventpro.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "clients")
@Getter @Setter @NoArgsConstructor
public class Client extends User {
//    Hibernate id — Primary key (PK) and foreign key (FK) because heritage form User 
    private String telephone;

@OneToMany(mappedBy = "client")
private List<Reservation> reservations = new ArrayList<>();
}
