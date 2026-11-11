package com.eventpro.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

// nom = name of the organizer / organization (no more nomOrganisation)
@Builder
public record OrganisateurUpdateDTO(
        @NotBlank(message = "Le nom est obligatoire") String nom,
        @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") String email
) {
}
