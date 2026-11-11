package com.eventpro.dto.user;

import lombok.Builder;

import java.time.LocalDate;

/** One response for every kind of account. The password hash is never exposed. */
@Builder
public record UtilisateurResponseDTO(
        Long id,
        String nom,
        String email,
        boolean actif,
        LocalDate dateInscription,
        String role            // CLIENT, ORGANISATEUR, ADMINISTRATEUR, AGENT_CONTROLE
) {
}
