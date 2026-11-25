package com.eventpro.dto.remboursement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RemboursementRequestDTO(

        @NotBlank(message = "Le motif est obligatoire")
        @Size(max = 255, message = "Le motif ne doit pas depasser 255 caracteres")
        String motif
) {}