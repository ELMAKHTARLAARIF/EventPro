package com.eventpro.entity;

import com.eventpro.enums.StatutBillet;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    // is the ticket usable?
    public boolean valider() {
        return statut == StatutBillet.VALIDE;
    }

    // composts the ticket: VALIDE -> UTILISE, once only
    public boolean controler() {
        if (statut != StatutBillet.VALIDE) {
            return false;
        }
        statut = StatutBillet.UTILISE;
        dateControle = LocalDateTime.now();
        return true;
    }
}