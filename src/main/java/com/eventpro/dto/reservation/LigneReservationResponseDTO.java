package com.eventpro.dto.reservation;

import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record LigneReservationResponseDTO(
        Long id,
        Long categorieId,
        String categorieNom,
        int quantite,
        BigDecimal prixUnitaire,
        BigDecimal sousTotal
) {}