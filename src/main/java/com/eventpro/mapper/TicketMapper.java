package com.eventpro.mapper;

import com.eventpro.dto.TicketResponseDTO;
import com.eventpro.entity.Billet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TicketMapper {

    @Mapping(target = "categorieNom", source = "ligneReservation.categorie.nom")
    @Mapping(target = "evenementTitre", source = "ligneReservation.reservation.evenement.titre")
    @Mapping(target = "porteur", source = "ligneReservation.reservation.client.nom")
    TicketResponseDTO toResponse(Billet billet);

    List<TicketResponseDTO> toResponseList(List<Billet> billets);
}