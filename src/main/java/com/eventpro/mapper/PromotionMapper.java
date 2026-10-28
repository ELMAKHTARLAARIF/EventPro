package com.eventpro.mapper;

import com.eventpro.dto.EvenementAvecPromotionsDTO;
import com.eventpro.dto.PromotionRequestDTO;
import com.eventpro.dto.PromotionResponseDTO;
import com.eventpro.dto.PromotionUpdateDTO;
import com.eventpro.entity.Evenement;
import com.eventpro.entity.Promotion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PromotionMapper {

    // Entity -> Response
    @Mapping(target = "evenementId", source = "evenement.id")
    PromotionResponseDTO toResponse(Promotion promotion);

    List<PromotionResponseDTO> toResponseList(List<Promotion> promotions);

    // Evenement -> Evenement with its promotions
    // the "promotions" list is converted with toResponse() above, automatically
    EvenementAvecPromotionsDTO toEvenementAvecPromotions(Evenement evenement);

    List<EvenementAvecPromotionsDTO> toEvenementAvecPromotionsList(List<Evenement> evenements);

    // Request -> new Entity (create)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "nombreUtilisations", ignore = true)  // starts at 0
    @Mapping(target = "evenement", ignore = true)           // set by the service
    @Mapping(target = "organisateur", ignore = true)        // taken from the event
    @Mapping(target = "reservations", ignore = true)
    @Mapping(target = "code", expression = "java(dto.code().trim().toUpperCase())")
    Promotion toEntity(PromotionRequestDTO dto);

    // Update DTO -> existing Entity (update)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "nombreUtilisations", ignore = true)  // never changed by the client
    @Mapping(target = "evenement", ignore = true)
    @Mapping(target = "organisateur", ignore = true)
    @Mapping(target = "reservations", ignore = true)
    @Mapping(target = "code", expression = "java(dto.code().trim().toUpperCase())")
    void updateEntity(PromotionUpdateDTO dto, @MappingTarget Promotion promotion);
}