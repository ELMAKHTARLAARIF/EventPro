package com.eventpro.dto;

import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record CategorieBilletResponseDTO(
        Long id,
        String nom,
        BigDecimal prixUnitaire,
        int quantiteTotal,
        int quotaMaxParClient,
        Long evenementId
) {}