package com.eventpro.entity;

import com.eventpro.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "categories_billet")
@Getter @Setter @NoArgsConstructor
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evenement_id")
    private Evenement evenement;
}
