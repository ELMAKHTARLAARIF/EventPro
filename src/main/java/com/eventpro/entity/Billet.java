package com.eventpro.entity;

import com.eventpro.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "billets")
@Getter @Setter @NoArgsConstructor
public class Billet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    private LocalDate dateEmission = LocalDate.now();

    // filled when scanned at the entrance
    private LocalDateTime dateControle;

    @Enumerated(EnumType.STRING)
    private StatutBillet statut = StatutBillet.VALIDE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ligne_reservation_id")
    private LigneReservation ligneReservation;

    @OneToMany(mappedBy = "billet")
    private List<ControleBillet> controles = new ArrayList<>();
}
