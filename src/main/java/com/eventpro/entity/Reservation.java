package com.eventpro.entity;

import com.eventpro.enums.StatutReservation;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reference;

    private LocalDateTime dateCreation = LocalDateTime.now();

    // dateCreation + 10 minutes (cahier des charges)
    private LocalDateTime dateExpiration;

    @Enumerated(EnumType.STRING)
    private StatutReservation statut = StatutReservation.EN_ATTENTE;

    @Column(precision = 10, scale = 2)
    private BigDecimal montantTotal = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evenement_id")
    private Evenement evenement;

    // optional: a reservation may have no promo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneReservation> lignes = new ArrayList<>();

    @OneToOne(mappedBy = "reservation")
    private Paiement paiement;

    public void addLigne(LigneReservation l) {
        lignes.add(l);
        l.setReservation(this);
    }

    public BigDecimal calculerTotal() {
        return lignes.stream().map(LigneReservation::getSousTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}