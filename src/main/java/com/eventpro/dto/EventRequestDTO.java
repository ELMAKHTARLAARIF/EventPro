package com.eventpro.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record EventRequestDTO(

        @NotBlank(message = "Le titre est obligatoire") @Size(max = 150, message = "Le titre ne doit pas depasser 150 caracteres") String titre,

        @Size(max = 2000, message = "La description ne doit pas depasser 2000 caracteres") String description,

        @NotBlank(message = "Le lieu est obligatoire") @Size(max = 150, message = "Le lieu ne doit pas depasser 150 caracteres") String lieu,

        @NotNull(message = "La date de debut est obligatoire") LocalDateTime dateDebut,

        @NotNull(message = "La date de fin est obligatoire") LocalDateTime dateFin,

        @NotNull(message = "La date d'ouverture des ventes est obligatoire") LocalDateTime dateOuvertureVente,

        @NotNull(message = "La date de cloture des ventes est obligatoire") LocalDateTime dateClotureVente,

        @NotNull(message = "La capacite est obligatoire") @Min(value = 1, message = "La capacite doit etre au moins 1") Integer capacite,

        // required on create only (checked in the service)
        Long organisateurId) {

    @JsonIgnore
    @AssertTrue(message = "La date de fin doit etre apres la date de debut")
    public boolean isPeriodeEvenementValide() {
        return dateDebut == null || dateFin == null || dateFin.isAfter(dateDebut);
    }

    @JsonIgnore
    @AssertTrue(message = "La cloture des ventes doit etre apres l'ouverture des ventes")
    public boolean isPeriodeVenteValide() {
        return dateOuvertureVente == null || dateClotureVente == null || dateClotureVente.isAfter(dateOuvertureVente);
    }

    @JsonIgnore
    @AssertTrue(message = "La cloture des ventes ne peut pas etre apres la fin de l'evenement")
    public boolean isClotureAvantFinEvenement() {
        return dateClotureVente == null || dateFin == null || !dateClotureVente.isAfter(dateFin);
    }
}