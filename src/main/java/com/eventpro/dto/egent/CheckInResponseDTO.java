package com.eventpro.dto;

import com.eventpro.enums.ResultatControle;
import com.eventpro.enums.StatutBillet;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CheckInResponseDTO(
        String code,
        ResultatControle resultat,       // VALIDE, DEJA_UTILISE or INVALIDE
        String message,
        StatutBillet statutBillet,
        LocalDateTime dateScan,          // null for a simple verification
        String categorieNom,
        Long evenementId,
        String evenementTitre,
        String porteur
) {}