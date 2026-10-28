package com.eventpro.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LigneReservationRequestDTO(

        @NotNull(message = "La categorie est obligatoire") Long categorieId,

        @NotNull(message = "La quantite est obligatoire") @Min(value = 1, message = "La quantite doit etre au moins 1") Integer quantite) {
}