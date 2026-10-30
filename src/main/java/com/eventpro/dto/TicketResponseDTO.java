package com.eventpro.dto;

import com.eventpro.enums.StatutBillet;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record TicketResponseDTO(
        Long id,
        String code,
        StatutBillet statut,
        LocalDate dateEmission,
        LocalDateTime dateControle,
        String categorieNom,
        String evenementTitre,
        String porteur
) {}