package com.eventpro.dto.category_billet;

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