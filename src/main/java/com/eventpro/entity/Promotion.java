package com.eventpro.entity;

import com.eventpro.enums.TypeReduction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "promotions", uniqueConstraints = @UniqueConstraint(columnNames = {"code", "evenement_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // unique per event, not globally
    @Column(nullable = false, length = 30)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeReduction typeReduction;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valeurReduction;

    private LocalDate dateDebut;
    private LocalDate dateFin;

    private int limiteUtilisation;
    private int nombreUtilisations;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evenement_id")
    private Evenement evenement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisateur_id")
    private Organisateur organisateur;

    @OneToMany(mappedBy = "promotion")
    private List<Reservation> reservations = new ArrayList<>();

    public boolean estUtilisable() {
        LocalDate today = LocalDate.now();
        return !today.isBefore(dateDebut) && !today.isAfter(dateFin) && nombreUtilisations < limiteUtilisation;
    }

    public BigDecimal calculerReduction(BigDecimal sousTotal) {
        BigDecimal remise = (typeReduction == TypeReduction.POURCENTAGE) ? sousTotal.multiply(valeurReduction).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP) : valeurReduction;
        return remise.min(sousTotal);
    }
}