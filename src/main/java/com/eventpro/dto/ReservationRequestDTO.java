package com.eventpro.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;
import java.util.Objects;

public record ReservationRequestDTO(

        @NotNull(message = "L'evenement est obligatoire") Long evenementId,

        // temporary: will come from the logged-in user once security is added
        @NotNull(message = "Le client est obligatoire") Long clientId,

        @NotEmpty(message = "Une reservation doit contenir au moins une ligne") @Valid List<LigneReservationRequestDTO> lignes,

        // optional: null is accepted, but if present it must have a valid format
        @Pattern(regexp = "^[A-Za-z0-9_-]{3,30}$", message = "Format de code promo invalide") String codePromo) {

    @JsonIgnore
    @AssertTrue(message = "Une meme categorie ne peut apparaitre qu'une seule fois")
    public boolean isCategoriesUniques() {
        if (lignes == null) {
            return true;
        }
        long distinctes = lignes.stream().filter(Objects::nonNull).map(LigneReservationRequestDTO::categorieId).distinct().count();
        return distinctes == lignes.size();
    }
}