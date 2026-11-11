package com.eventpro.mapper;

import com.eventpro.dto.event.EventRequestDTO;
import com.eventpro.dto.event.EventResponseDTO;
import com.eventpro.entity.Evenement;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "organisateurId", source = "organisateur.id")
    EventResponseDTO toResponse(Evenement evenement);

    List<EventResponseDTO> toResponseList(List<Evenement> evenements);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "organisateur", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "promotions", ignore = true)
    Evenement toEntity(EventRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "organisateur", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "promotions", ignore = true)
    void updateEntity(EventRequestDTO dto, @MappingTarget Evenement evenement);
}