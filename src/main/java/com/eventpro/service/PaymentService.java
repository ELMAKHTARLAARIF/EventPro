package com.eventpro.service;

import com.eventpro.dto.PaymentResponseDTO;
import com.eventpro.entity.Paiement;
import com.eventpro.entity.Reservation;
import com.eventpro.enums.StatutPaiement;
import com.eventpro.mapper.PaymentMapper;
import com.eventpro.repository.PaiementRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    // ---------- CREATE (simple version: the payment always succeeds) ----------
    @Transactional
    public Paiement creerPaiement(Reservation reservation) {

        Paiement paiement = new Paiement();
        paiement.setReference("PAY-" + Year.now().getValue() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        paiement.setMontant(reservation.getMontantTotal());
        paiement.setDatePaiement(LocalDateTime.now());
        paiement.setStatut(StatutPaiement.REUSSI);
        paiement.setReservation(reservation);

        paiementRepository.save(paiement);
        reservation.setPaiement(paiement);   // keeps both Java objects consistent
        return paiement;
    }

    // ---------- READ ----------
    @Transactional(readOnly = true)
    public PaymentResponseDTO findByReservation(Long reservationId) {
        return paiementRepository.findByReservationId(reservationId).map(paymentMapper::toResponse).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun paiement pour la reservation " + reservationId));
    }

    @Transactional(readOnly = true)
    public PaymentResponseDTO findByCleIdempotence(String cleIdempotence) {
        return paiementRepository.findByCleIdempotence(cleIdempotence.trim()).map(paymentMapper::toResponse).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun paiement pour la cle d'idempotence " + cleIdempotence));
    }
}