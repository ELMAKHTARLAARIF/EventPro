package com.eventpro.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;
import java.util.Objects;

public record ReservationUpdateDTO(

        @NotEmpty(message = "Une reservation doit contenir au moins une ligne") @Valid List<LigneReservationRequestDTO> lignes,

        // null = no promotion (removes the current one)
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