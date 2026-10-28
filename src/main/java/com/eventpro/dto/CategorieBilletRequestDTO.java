package com.eventpro.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CategorieBilletRequestDTO(

        @NotBlank(message = "Le nom est obligatoire") @Size(max = 100, message = "Le nom ne doit pas depasser 100 caracteres") String nom,

        @NotNull(message = "Le prix est obligatoire") @DecimalMin(value = "0.00", message = "Le prix ne peut pas etre negatif") @Digits(integer = 8, fraction = 2, message = "Format de prix invalide") BigDecimal prixUnitaire,

        @NotNull(message = "La quantite totale est obligatoire") @Min(value = 1, message = "La quantite totale doit etre au moins 1") Integer quantiteTotal,

        @NotNull(message = "Le quota par client est obligatoire") @Min(value = 1, message = "Le quota par client doit etre au moins 1") Integer quotaMaxParClient) {

    @JsonIgnore
    @AssertTrue(message = "Le quota par client ne peut pas depasser la quantite totale")
    public boolean isQuotaValide() {
        return quantiteTotal == null || quotaMaxParClient == null || quotaMaxParClient <= quantiteTotal;
    }
}