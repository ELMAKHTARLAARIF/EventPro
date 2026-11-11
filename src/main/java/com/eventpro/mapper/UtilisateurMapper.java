package com.eventpro.mapper;

import com.eventpro.dto.user.*;
import com.eventpro.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UtilisateurMapper {

    // ---------- Entity -> Response (one method for all accounts) ----------
    @Mapping(target = "role", expression = "java(roleDe(user))")
    UtilisateurResponseDTO toResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "motDePasse", ignore = true)
    Client toClient(ClientRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "motDePasse", ignore = true)
    Organisateur toOrganisateur(OrganisateurRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "motDePasse", ignore = true)
    Administrateur toAdministrateur(AdministrateurRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "motDePasse", ignore = true)
    AgentControle toAgentControle(AgentControleRequestDTO dto);

    void updateClient(ClientUpdateDTO dto, @MappingTarget Client client);

    void updateOrganisateur(OrganisateurUpdateDTO dto, @MappingTarget Organisateur organisateur);

    // ---------- helper ----------
    default String roleDe(User user) {
        if (user instanceof Client) return "CLIENT";
        if (user instanceof Organisateur) return "ORGANISATEUR";
        if (user instanceof Administrateur) return "ADMINISTRATEUR";
        if (user instanceof AgentControle) return "AGENT_CONTROLE";
        return "UTILISATEUR";
    }
}