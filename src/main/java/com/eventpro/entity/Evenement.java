package com.eventpro.entity;

import com.eventpro.enums.StatutEvenement;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evenements")
@Getter
@Setter
@NoArgsConstructor
public class Evenement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(length = 2000)
    private String description;

    private String lieu;

    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private LocalDateTime dateOuvertureVente;
    private LocalDateTime dateClotureVente;

    @Enumerated(EnumType.STRING)
    private StatutEvenement statut = StatutEvenement.BROUILLON;

    private int capacite;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisateur_id")
    private Organisateur organisateur;

    @OneToMany(mappedBy = "evenement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CategorieBillet> categories = new ArrayList<>();

    @OneToMany(mappedBy = "evenement")
    private List<Promotion> promotions = new ArrayList<>();

    public void addCategorie(CategorieBillet c) {
        categories.add(c);
        c.setEvenement(this);
    }

}