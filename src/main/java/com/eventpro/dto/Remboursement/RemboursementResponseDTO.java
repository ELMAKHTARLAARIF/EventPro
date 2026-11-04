package com.eventpro.dto.Remboursement;

import com.eventpro.enums.StatutRemboursement;
import com.eventpro.enums.StatutReservation;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record RemboursementResponseDTO(
        Long id,
        BigDecimal montant,
        String motif,
        LocalDateTime dateDemande,
        StatutRemboursement statut,
        Long paiementId,
        Long reservationId,
        StatutReservation reservationStatut
) {}