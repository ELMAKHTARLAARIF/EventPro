package com.eventpro.dto.event;

import com.eventpro.enums.StatutEvenement;
import java.time.LocalDateTime;

public record EventResponseDTO(
        Long id,
        String titre,
        String description,
        String lieu,
        LocalDateTime dateDebut,
        LocalDateTime dateFin,
        LocalDateTime dateOuvertureVente,
        LocalDateTime dateClotureVente,
        StatutEvenement statut,
        int capacite,
        Long organisateurId,
        String nomOrganisation
) {}