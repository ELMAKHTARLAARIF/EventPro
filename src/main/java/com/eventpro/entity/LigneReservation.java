package com.eventpro.entity;

import com.eventpro.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "lignes_reservation")
@Getter @Setter @NoArgsConstructor
public class LigneReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int quantite;

    // copied from CategorieBillet at booking time
    @Column(precision = 10, scale = 2)
    private BigDecimal prixUnitaire;

    @Column(precision = 10, scale = 2)
    private BigDecimal sousTotal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categorie_id")
    private CategorieBillet categorie;

    @OneToMany(mappedBy = "ligneReservation")
    private List<Billet> billets = new ArrayList<>();

    @PrePersist
    @PreUpdate
    void calculerSousTotal() {
        if (prixUnitaire != null) {
            sousTotal = prixUnitaire.multiply(BigDecimal.valueOf(quantite));
        }
    }
}
