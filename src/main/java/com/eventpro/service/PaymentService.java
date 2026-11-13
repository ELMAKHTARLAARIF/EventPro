package com.eventpro.service;

import com.eventpro.dto.payment.PaymentResponseDTO;
import com.eventpro.entity.Paiement;
import com.eventpro.entity.Reservation;
import com.eventpro.enums.StatutPaiement;
// EXCEPTION: our custom business exception (handled by GlobalExceptionHandler)
import com.eventpro.exception.ResourceNotFoundException;
import com.eventpro.mapper.PaymentMapper;
import com.eventpro.repository.PaiementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaiementRepository paiementRepository;
    private final PaymentMapper paymentMapper;

    public PaymentService(PaiementRepository paiementRepository, PaymentMapper paymentMapper) {
        this.paiementRepository = paiementRepository;
        this.paymentMapper = paymentMapper;
    }

    @Transactional
    public Paiement creerPaiement(Reservation reservation) {

        Paiement paiement = new Paiement();
        paiement.setReference("PAY-" + Year.now().getValue() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        paiement.setMontant(reservation.getMontantTotal());
        paiement.setDatePaiement(LocalDateTime.now());
        paiement.setCleIdempotence("ABCDEFG");
        paiement.setStatut(StatutPaiement.REUSSI);
        paiement.setReservation(reservation);

        paiementRepository.save(paiement);
        reservation.setPaiement(paiement);
        return paiement;
    }

    @Transactional(readOnly = true)
    public PaymentResponseDTO findByReservation(Long reservationId) {
        return paiementRepository.findByReservationId(reservationId).map(paymentMapper::toResponse).orElseThrow(() -> new ResourceNotFoundException("Aucun paiement pour la reservation " + reservationId));
    }

    @Transactional(readOnly = true)
    public PaymentResponseDTO findByCleIdempotence(String cleIdempotence) {
        return paiementRepository.findByCleIdempotence(cleIdempotence.trim()).map(paymentMapper::toResponse).orElseThrow(() -> new ResourceNotFoundException("Aucun paiement pour la cle d'idempotence " + cleIdempotence));
    }
}