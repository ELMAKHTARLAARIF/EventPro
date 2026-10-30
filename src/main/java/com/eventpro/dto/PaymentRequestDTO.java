package com.eventpro.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PaymentRequestDTO(

        @NotBlank(message = "La cle d'idempotence est obligatoire")
        @Size(max = 100, message = "La cle d'idempotence ne doit pas depasser 100 caracteres")
        String cleIdempotence,

        @NotNull(message = "Le montant est obligatoire")
        @DecimalMin(value = "0.00", message = "Le montant ne peut pas etre negatif")
        BigDecimal montant
) {}