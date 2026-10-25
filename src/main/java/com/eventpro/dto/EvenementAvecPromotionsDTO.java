package com.eventpro.dto;

import com.eventpro.enums.StatutEvenement;
import java.time.LocalDateTime;
import java.util.List;

public record EvenementAvecPromotionsDTO(
        Long id,
        String titre,
        String lieu,
        LocalDateTime dateDebut,
        LocalDateTime dateFin,
        StatutEvenement statut,
        List<PromotionResponseDTO> promotions
) {}