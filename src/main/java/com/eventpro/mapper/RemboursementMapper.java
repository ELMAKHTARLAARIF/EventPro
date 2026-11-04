package com.eventpro.mapper;

import com.eventpro.dto.Remboursement.RemboursementResponseDTO;
import com.eventpro.entity.Remboursement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RemboursementMapper {

    @Mapping(target = "paiementId", source = "paiement.id")
    @Mapping(target = "reservationId", source = "paiement.reservation.id")
    @Mapping(target = "reservationStatut", source = "paiement.reservation.statut")
    RemboursementResponseDTO toResponse(Remboursement remboursement);

    List<RemboursementResponseDTO> toResponseList(List<Remboursement> remboursements);
}