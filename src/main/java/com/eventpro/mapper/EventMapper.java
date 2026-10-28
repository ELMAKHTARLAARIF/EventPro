package com.eventpro.mapper;

import com.eventpro.dto.EventRequestDTO;
import com.eventpro.dto.EventResponseDTO;
import com.eventpro.entity.Evenement;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EventMapper {

    // Entity -> Response
    @Mapping(target = "organisateurId", source = "organisateur.id")
    @Mapping(target = "nomOrganisation", source = "organisateur.nomOrganisation")
    EventResponseDTO toResponse(Evenement evenement);

    List<EventResponseDTO> toResponseList(List<Evenement> evenements);

    // Request -> new Entity (used by create)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "statut", ignore = true)        // set by the service (DRAFT)
    @Mapping(target = "organisateur", ignore = true)  // loaded by the service from organisateurId
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "promotions", ignore = true)
    Evenement toEntity(EventRequestDTO dto);

    // Request -> existing Entity (used by update)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "organisateur", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "promotions", ignore = true)
    void updateEntity(EventRequestDTO dto, @MappingTarget Evenement evenement);
}