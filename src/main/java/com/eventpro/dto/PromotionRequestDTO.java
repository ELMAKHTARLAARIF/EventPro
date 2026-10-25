package com.eventpro.dto;

import com.eventpro.enums.TypeReduction;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PromotionRequestDTO(

        @NotBlank(message = "Le code est obligatoire")
        @Pattern(regexp = "^[A-Za-z0-9_-]{3,30}$",
                 message = "Le code doit contenir 3 a 30 caracteres (lettres, chiffres, - ou _)")
        String code,

        @NotNull(message = "Le type de reduction est obligatoire")
        TypeReduction typeReduction,

        @NotNull(message = "La valeur de reduction est obligatoire")
        @DecimalMin(value = "0.01", message = "La valeur doit etre superieure a 0")
        @Digits(integer = 8, fraction = 2, message = "Format de valeur invalide")
        BigDecimal valeurReduction,

        @NotNull(message = "La date de debut est obligatoire")
        @FutureOrPresent(message = "La date de debut ne peut pas etre dans le passe")
        LocalDate dateDebut,

        @NotNull(message = "La date de fin est obligatoire")
        LocalDate dateFin,

        @NotNull(message = "La limite d'utilisation est obligatoire")
        @Min(value = 1, message = "La limite d'utilisation doit etre au moins 1")
        Integer limiteUtilisation
) {

    @JsonIgnore
    @AssertTrue(message = "La date de fin doit etre apres ou egale a la date de debut")
    public boolean isPeriodeValide() {
        return dateDebut == null || dateFin == null || !dateFin.isBefore(dateDebut);
    }

    @JsonIgnore
    @AssertTrue(message = "Un pourcentage doit etre compris entre 0 et 100")
    public boolean isPourcentageValide() {
        if (typeReduction != TypeReduction.POURCENTAGE || valeurReduction == null) {
            return true;
        }
        return valeurReduction.compareTo(BigDecimal.valueOf(100)) <= 0;
    }
}
