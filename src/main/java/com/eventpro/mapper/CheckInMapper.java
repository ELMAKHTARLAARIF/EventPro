package com.eventpro.mapper;

import com.eventpro.dto.CheckInResponseDTO;
import com.eventpro.entity.Billet;
import com.eventpro.enums.ResultatControle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface CheckInMapper {

    @Mapping(target = "code", source = "billet.code")
    @Mapping(target = "statutBillet", source = "billet.statut")
    @Mapping(target = "categorieNom", source = "billet.ligneReservation.categorie.nom")
    @Mapping(target = "evenementId", source = "billet.ligneReservation.reservation.evenement.id")
    @Mapping(target = "evenementTitre", source = "billet.ligneReservation.reservation.evenement.titre")
    @Mapping(target = "porteur", source = "billet.ligneReservation.reservation.client.nom")
    @Mapping(target = "resultat", source = "resultat")
    @Mapping(target = "dateScan", source = "dateScan")
    @Mapping(target = "message", source = "message")
    CheckInResponseDTO toResponse(Billet billet, ResultatControle resultat, LocalDateTime dateScan, String message);
}