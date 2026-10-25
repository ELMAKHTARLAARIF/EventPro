package com.eventpro.entity;

import com.eventpro.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "controles_billet")
@Getter @Setter @NoArgsConstructor
public class ControleBillet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime dateScan = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private ResultatControle resultat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "billet_id")
    private Billet billet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id")
    private AgentControle agent;
}
