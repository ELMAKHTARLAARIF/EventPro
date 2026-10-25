package com.eventpro.entity;

import com.eventpro.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "remboursements")
@Getter @Setter @NoArgsConstructor
public class Remboursement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(precision = 10, scale = 2)
    private BigDecimal montant;

    private String motif;

    private LocalDateTime dateDemande = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private StatutRemboursement statut = StatutRemboursement.EN_ATTENTE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paiement_id")
    private Paiement paiement;
}
