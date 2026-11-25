
package com.eventpro.service;

import com.eventpro.dto.remboursement.RemboursementRequestDTO;
import com.eventpro.dto.remboursement.RemboursementResponseDTO;
import com.eventpro.entity.*;
import com.eventpro.enums.StatutPaiement;
import com.eventpro.enums.StatutReservation;
import com.eventpro.exception.ResourceNotFoundException;
import com.eventpro.mapper.RemboursementMapper;
import com.eventpro.repository.RemboursementRepository;
import com.eventpro.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RemboursementServiceTest {

    @Mock ReservationRepository reservationRepository;
    @Mock RemboursementRepository remboursementRepository;
    @Mock CategorieBilletService categorieBilletService;
    @Mock TicketService ticketService;
    @Mock RemboursementMapper remboursementMapper;

    @InjectMocks RemboursementService service;

    // ---------- demander ----------

    @Test
    void demander_reservationInconnue_404() {
        when(reservationRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.demander(1L, mock(RemboursementRequestDTO.class)));
    }

    @Test
    void demander_reservationNonConfirmee_409() {
        Reservation r = mock(Reservation.class);

        when(reservationRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(r));
        when(r.getStatut()).thenReturn(StatutReservation.EN_ATTENTE);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.demander(1L, mock(RemboursementRequestDTO.class))
        );

        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void demander_sansPaiement_409() {
        Reservation r = mock(Reservation.class);

        when(reservationRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(r));
        when(r.getStatut()).thenReturn(StatutReservation.CONFIRMEE);
        when(r.getPaiement()).thenReturn(null);

        assertThrows(ResponseStatusException.class,
                () -> service.demander(1L, mock(RemboursementRequestDTO.class)));

        verifyNoInteractions(categorieBilletService, ticketService);
    }

    @Test
    void demander_moinsDe48hAvantLEvenement_refuse_montantZero_billetsConserves() {
        Reservation r = reservationConfirmee(LocalDateTime.now().plusHours(10));
        RemboursementRequestDTO dto = requete(" Imprevu ");

        when(remboursementRepository.save(any(Remboursement.class)))
                .thenAnswer(i -> i.getArgument(0));

        when(remboursementMapper.toResponse(any(Remboursement.class)))
                .thenReturn(mock(RemboursementResponseDTO.class));

        service.demander(1L, dto);

        ArgumentCaptor<Remboursement> captor =
                ArgumentCaptor.forClass(Remboursement.class);

        verify(remboursementRepository).save(captor.capture());

        Remboursement remboursement = captor.getValue();

        assertEquals(0, BigDecimal.ZERO.compareTo(remboursement.getMontant()));
        assertEquals("Imprevu", remboursement.getMotif());

        verifyNoInteractions(categorieBilletService, ticketService);
        verify(r, never()).setStatut(StatutReservation.REMBOURSEE);
    }

    @Test
    void demander_48hOuPlusAvantLEvenement_remboursementTotal() {
        Reservation r = reservationConfirmee(LocalDateTime.now().plusDays(5));

        Paiement paiement = r.getPaiement();
        when(paiement.getMontant()).thenReturn(new BigDecimal("200.00"));

        Evenement evenement = r.getEvenement();
        when(evenement.getId()).thenReturn(7L);

        CategorieBillet categorie = new CategorieBillet();
        categorie.setId(1L);

        LigneReservation ligne = new LigneReservation();
        ligne.setCategorie(categorie);
        ligne.setQuantite(2);

        when(r.getLignes()).thenReturn(List.of(ligne));

        when(categorieBilletService.verrouiller(anyCollection(), eq(7L)))
                .thenReturn(Map.of(1L, categorie));

        when(remboursementRepository.save(any(Remboursement.class)))
                .thenAnswer(i -> i.getArgument(0));

        when(remboursementMapper.toResponse(any(Remboursement.class)))
                .thenReturn(mock(RemboursementResponseDTO.class));

        service.demander(1L, requete("Changement de plans"));

        ArgumentCaptor<Remboursement> captor =
                ArgumentCaptor.forClass(Remboursement.class);

        verify(remboursementRepository).save(captor.capture());

        assertEquals(
                0,
                new BigDecimal("200.00").compareTo(captor.getValue().getMontant())
        );

        verify(categorieBilletService).annulerVente(categorie, 2);
        verify(ticketService).annulerBillets(r);
        verify(r).setStatut(StatutReservation.REMBOURSEE);
    }

    // ---------- rembourserEvenementAnnule ----------

    @Test
    void rembourserEvenementAnnule_rembourseChaqueReservationConfirmee() {
        Evenement evenement = mock(Evenement.class);
        when(evenement.getId()).thenReturn(7L);

        Reservation r = mock(Reservation.class);
        Paiement paiement = mock(Paiement.class);

        when(paiement.getMontant()).thenReturn(new BigDecimal("100.00"));
        when(r.getPaiement()).thenReturn(paiement);
        when(r.getEvenement()).thenReturn(evenement);

        CategorieBillet categorie = new CategorieBillet();
        categorie.setId(1L);

        LigneReservation ligne = new LigneReservation();
        ligne.setCategorie(categorie);
        ligne.setQuantite(1);

        when(r.getLignes()).thenReturn(List.of(ligne));

        when(reservationRepository.findByEvenementAndStatutForUpdate(
                7L, StatutReservation.CONFIRMEE
        )).thenReturn(List.of(r));

        when(categorieBilletService.verrouiller(anyCollection(), eq(7L)))
                .thenReturn(Map.of(1L, categorie));

        service.rembourserEvenementAnnule(evenement);

        ArgumentCaptor<Remboursement> captor =
                ArgumentCaptor.forClass(Remboursement.class);

        verify(remboursementRepository).save(captor.capture());

        assertEquals(
                0,
                new BigDecimal("100.00").compareTo(captor.getValue().getMontant())
        );

        verify(ticketService).annulerBillets(r);
        verify(r).setStatut(StatutReservation.REMBOURSEE);
    }

    @Test
    void rembourserEvenementAnnule_aucuneReservation_rienNEstFait() {
        Evenement evenement = mock(Evenement.class);
        when(evenement.getId()).thenReturn(7L);

        when(reservationRepository.findByEvenementAndStatutForUpdate(
                7L, StatutReservation.CONFIRMEE
        )).thenReturn(List.of());

        service.rembourserEvenementAnnule(evenement);

        verifyNoInteractions(remboursementRepository, ticketService);
    }

    // ---------- findByReservation ----------

    @Test
    void findByReservation_inconnue_404() {
        when(reservationRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> service.findByReservation(1L));
    }

    @Test
    void findByReservation_ok() {
        List<Remboursement> liste = List.of(new Remboursement());

        List<RemboursementResponseDTO> dtos =
                List.of(mock(RemboursementResponseDTO.class));

        when(reservationRepository.existsById(1L)).thenReturn(true);

        when(remboursementRepository
                .findByPaiementReservationIdOrderByDateDemandeDesc(1L))
                .thenReturn(liste);

        when(remboursementMapper.toResponseList(liste)).thenReturn(dtos);

        assertSame(dtos, service.findByReservation(1L));
    }

    // ---------- helpers ----------

    private Reservation reservationConfirmee(LocalDateTime debutEvenement) {
        Reservation r = mock(Reservation.class);
        Paiement paiement = mock(Paiement.class);
        Evenement evenement = mock(Evenement.class);

        when(reservationRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(r));

        when(r.getStatut()).thenReturn(StatutReservation.CONFIRMEE);
        when(r.getPaiement()).thenReturn(paiement);
        when(r.getEvenement()).thenReturn(evenement);

        when(paiement.getStatut()).thenReturn(StatutPaiement.REUSSI);
        when(evenement.getDateDebut()).thenReturn(debutEvenement);

        return r;
    }

    private RemboursementRequestDTO requete(String motif) {
        RemboursementRequestDTO dto = mock(RemboursementRequestDTO.class);
        when(dto.motif()).thenReturn(motif);
        return dto;
    }
}
