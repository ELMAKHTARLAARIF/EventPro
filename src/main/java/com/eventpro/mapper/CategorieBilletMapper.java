package com.eventpro.mapper;

import com.eventpro.dto.CategorieBilletRequestDTO;
import com.eventpro.dto.CategorieBilletResponseDTO;
import com.eventpro.entity.CategorieBillet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategorieBilletMapper {

    @Mapping(target = "evenementId", source = "evenement.id")
    CategorieBilletResponseDTO toResponse(CategorieBillet categorie);

    List<CategorieBilletResponseDTO> toResponseList(List<CategorieBillet> categories);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "evenement", ignore = true)   // set by the service
    @Mapping(target = "nom", expression = "java(dto.nom().trim())")
    @Mapping(target = "placesReservees", ignore = true)
    @Mapping(target = "placesVendues", ignore = true)
    CategorieBillet toEntity(CategorieBilletRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "evenement", ignore = true)
    @Mapping(target = "nom", expression = "java(dto.nom().trim())")
    @Mapping(target = "placesReservees", ignore = true)
    @Mapping(target = "placesVendues", ignore = true)
    void updateEntity(CategorieBilletRequestDTO dto, @MappingTarget CategorieBillet categorie);
}