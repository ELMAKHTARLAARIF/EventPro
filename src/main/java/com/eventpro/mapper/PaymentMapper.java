package com.eventpro.mapper;

import com.eventpro.dto.PaymentResponseDTO;
import com.eventpro.entity.Paiement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "reservationId", source = "reservation.id")
    @Mapping(target = "reservationStatut", source = "reservation.statut")
    PaymentResponseDTO toResponse(Paiement paiement);
}