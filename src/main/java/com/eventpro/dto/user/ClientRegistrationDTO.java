package com.eventpro.dto.user;

public record ClientRegistrationDTO(
        String nom,
        String email,
        String motDePasse,
        String telephone
) {}