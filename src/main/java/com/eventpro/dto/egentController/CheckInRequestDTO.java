package com.eventpro.dto.egentController;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CheckInRequestDTO(

        @NotBlank(message = "Le code du billet est obligatoire")
        String code,

        @NotNull(message = "L'agent est obligatoire")
        Long agentId,

        // the event whose entrance the agent is guarding
        @NotNull(message = "L'evenement est obligatoire")
        Long evenementId
) {}