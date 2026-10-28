package com.eventpro.dto;

import com.eventpro.enums.StatutReservation;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ReservationResponseDTO(
        Long id,
        String reference,
        LocalDateTime dateCreation,
        LocalDateTime dateExpiration,      // the 10-minute deadline
        StatutReservation statut,
        Long evenementId,
        String evenementTitre,
        Long clientId,
        String codePromo,                  // null if no promotion
        List<LigneReservationResponseDTO> lignes,
        BigDecimal sousTotal,              // sum of the lines
        BigDecimal remise,                 // discount amount
        BigDecimal montantTotal            // sousTotal - remise
) {}