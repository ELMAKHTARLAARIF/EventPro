package com.eventpro.mapper;

import com.eventpro.dto.LigneReservationResponseDTO;
import com.eventpro.dto.ReservationResponseDTO;
import com.eventpro.entity.LigneReservation;
import com.eventpro.entity.Reservation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReservationMapper {

    @Mapping(target = "evenementId", source = "evenement.id")
    @Mapping(target = "evenementTitre", source = "evenement.titre")
    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "codePromo", source = "promotion.code")
    @Mapping(target = "sousTotal", expression = "java(reservation.calculerTotal())")
    @Mapping(target = "remise",
            expression = "java(reservation.calculerTotal().subtract(reservation.getMontantTotal()))")
    ReservationResponseDTO toResponse(Reservation reservation);

    List<ReservationResponseDTO> toResponseList(List<Reservation> reservations);

    @Mapping(target = "categorieId", source = "categorie.id")
    @Mapping(target = "categorieNom", source = "categorie.nom")
    LigneReservationResponseDTO toLigneResponse(LigneReservation ligne);
}