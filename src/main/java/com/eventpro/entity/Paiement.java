package com.eventpro.entity;

import com.eventpro.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "paiements")
@Getter @Setter @NoArgsConstructor
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(precision = 10, scale = 2)
    private BigDecimal montant;

    private LocalDateTime datePaiement;

    @Enumerated(EnumType.STRING)
    private StatutPaiement statut = StatutPaiement.EN_ATTENTE;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", unique = true)
    private Reservation reservation;

    @OneToMany(mappedBy = "paiement")
    private List<Remboursement> remboursements = new ArrayList<>();
}
