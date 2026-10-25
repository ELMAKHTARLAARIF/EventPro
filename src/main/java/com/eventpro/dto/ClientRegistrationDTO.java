package com.eventpro.dto;

public record ClientRegistrationDTO(
        String nom,
        String email,
        String motDePasse,
        String telephone
) {}