package com.eventpro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "categories_billet")
@Getter
@Setter
@NoArgsConstructor
public class CategorieBillet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prixUnitaire;

    private int quantiteTotal;

    private int quotaMaxParClient;
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int placesReservees = 0;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int placesVendues = 0;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evenement_id")
    private Evenement evenement;

    public int placesDisponibles() {
        return quantiteTotal - (placesReservees + placesVendues);
    }
}
