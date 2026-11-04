package com.eventpro.dto.user;

public record OrganisateurRegistrationDTO(
        String nom,
        String email,
        String motDePasse,
        String nomOrganisation
) {}