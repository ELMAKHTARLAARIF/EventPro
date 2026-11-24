package com.eventpro.service;

import com.eventpro.dto.payment.PaymentResponseDTO;
import com.eventpro.entity.Paiement;
import com.eventpro.entity.Reservation;
import com.eventpro.enums.StatutPaiement;
import com.eventpro.exception.ResourceNotFoundException;
import com.eventpro.mapper.PaymentMapper;
import com.eventpro.repository.PaiementRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaiementRepository paiementRepository;
    @Mock private PaymentMapper paymentMapper;

    @InjectMocks private PaymentService paymentService;

    private Reservation reservation(String montant) {
        Reservation reservation = new Reservation();
        reservation.setMontantTotal(new BigDecimal(montant));
        return reservation;
    }

    // ---------- creerPaiement ----------
    @Test
    @DisplayName("creerPaiement : payment is SUCCESS with the reservation amount")
    void creerPaiement_montantEtStatut() {
        Reservation reservation = reservation("150.00");

        Paiement paiement = paymentService.creerPaiement(reservation);

        assertEquals(StatutPaiement.REUSSI, paiement.getStatut());
        assertEquals(0, new BigDecimal("150.00").compareTo(paiement.getMontant()));
        assertSame(reservation, paiement.getReservation());
    }

    @Test
    @DisplayName("creerPaiement : reference has the PAY-YYYY-XXXXXXXX format")
    void creerPaiement_formatReference() {
        Paiement paiement = paymentService.creerPaiement(reservation("10.00"));

        assertTrue(paiement.getReference().matches("PAY-\\d{4}-[A-F0-9]{8}"), paiement.getReference());
    }

    @Test
    @DisplayName("creerPaiement : payment date is now")
    void creerPaiement_datePaiement() {
        LocalDateTime avant = LocalDateTime.now().minusSeconds(1);

        Paiement paiement = paymentService.creerPaiement(reservation("10.00"));

        assertFalse(paiement.getDatePaiement().isBefore(avant));
        assertFalse(paiement.getDatePaiement().isAfter(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    @DisplayName("creerPaiement : saves the payment and links it to the reservation")
    void creerPaiement_sauvegardeEtLien() {
        Reservation reservation = reservation("99.90");

        Paiement paiement = paymentService.creerPaiement(reservation);

        ArgumentCaptor<Paiement> captor = ArgumentCaptor.forClass(Paiement.class);
        verify(paiementRepository, times(1)).save(captor.capture());
        assertSame(paiement, captor.getValue());
        assertSame(paiement, reservation.getPaiement());
    }

    @Test
    @DisplayName("creerPaiement : two payments get two different references")
    void creerPaiement_referencesDifferentes() {
        Paiement p1 = paymentService.creerPaiement(reservation("10.00"));
        Paiement p2 = paymentService.creerPaiement(reservation("10.00"));

        assertNotEquals(p1.getReference(), p2.getReference());
    }

    @Test
    @DisplayName("creerPaiement : a free reservation (0 MAD) is accepted")
    void creerPaiement_montantZero() {
        Paiement paiement = paymentService.creerPaiement(reservation("0.00"));

        assertEquals(0, BigDecimal.ZERO.compareTo(paiement.getMontant()));
        assertEquals(StatutPaiement.REUSSI, paiement.getStatut());
    }

    // ---------- findByReservation ----------
    @Test
    @DisplayName("findByReservation : returns the mapped payment")
    void findByReservation_ok() {
        Paiement paiement = new Paiement();
        PaymentResponseDTO reponse = mock(PaymentResponseDTO.class);
        when(paiementRepository.findByReservationId(1L)).thenReturn(Optional.of(paiement));
        when(paymentMapper.toResponse(paiement)).thenReturn(reponse);

        assertSame(reponse, paymentService.findByReservation(1L));
    }

    @Test
    @DisplayName("findByReservation : no payment -> ResourceNotFoundException")
    void findByReservation_introuvable() {
        when(paiementRepository.findByReservationId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.findByReservation(1L));
    }

    // ---------- findByCleIdempotence ----------
    @Test
    @DisplayName("findByCleIdempotence : the key is trimmed before the search")
    void findByCle_trim() {
        Paiement paiement = new Paiement();
        PaymentResponseDTO reponse = mock(PaymentResponseDTO.class);
        when(paiementRepository.findByCleIdempotence("KEY-1")).thenReturn(Optional.of(paiement));
        when(paymentMapper.toResponse(paiement)).thenReturn(reponse);

        assertSame(reponse, paymentService.findByCleIdempotence("  KEY-1  "));

        verify(paiementRepository).findByCleIdempotence("KEY-1");
    }

    @Test
    @DisplayName("findByCleIdempotence : unknown key -> ResourceNotFoundException")
    void findByCle_introuvable() {
        when(paiementRepository.findByCleIdempotence("NOPE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.findByCleIdempotence("NOPE"));
    }
}
