package com.eventpro.dto;

import com.eventpro.enums.StatutPaiement;
import com.eventpro.enums.StatutReservation;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record PaymentResponseDTO(
        Long id,
        String reference,
        BigDecimal montant,
        LocalDateTime datePaiement,
        StatutPaiement statut,
        Long reservationId,
        StatutReservation reservationStatut
) {}