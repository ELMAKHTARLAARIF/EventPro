package com.eventpro.dto;

import com.eventpro.enums.TypeReduction;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PromotionResponseDTO(
        Long id,
        String code,
        TypeReduction typeReduction,
        BigDecimal valeurReduction,
        LocalDate dateDebut,
        LocalDate dateFin,
        int limiteUtilisation,
        int nombreUtilisations,
        Long evenementId
) {}
