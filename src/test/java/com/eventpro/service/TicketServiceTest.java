package com.eventpro.service;

import com.eventpro.dto.TicketResponseDTO;
import com.eventpro.entity.Billet;
import com.eventpro.entity.LigneReservation;
import com.eventpro.entity.Reservation;
import com.eventpro.enums.StatutBillet;
import com.eventpro.exception.ResourceNotFoundException;
import com.eventpro.mapper.TicketMapper;
import com.eventpro.repository.BilletRepository;
import com.eventpro.repository.ReservationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock private BilletRepository billetRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private TicketMapper ticketMapper;

    @InjectMocks private TicketService ticketService;

    // ---------- helpers ----------
    private Reservation reservationAvecLignes(int... quantites) {
        Reservation reservation = new Reservation();
        for (int quantite : quantites) {
            LigneReservation ligne = new LigneReservation();
            ligne.setQuantite(quantite);
            reservation.addLigne(ligne);
        }
        return reservation;
    }

    private Billet billet(StatutBillet statut) {
        Billet billet = new Billet();
        billet.setStatut(statut);
        return billet;
    }

    // ---------- genererBillets ----------
    @Test
    @DisplayName("genererBillets : one ticket per purchased unit, all lines included")
    void genererBillets_creeUnBilletParPlace() {
        Reservation reservation = reservationAvecLignes(2, 3);
        when(billetRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Billet> billets = ticketService.genererBillets(reservation);

        assertEquals(5, billets.size());
    }

    @Test
    @DisplayName("genererBillets : every ticket is VALIDE and linked to its line")
    void genererBillets_statutValideEtLigneAssociee() {
        Reservation reservation = reservationAvecLignes(2);
        LigneReservation ligne = reservation.getLignes().get(0);
        when(billetRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Billet> billets = ticketService.genererBillets(reservation);

        for (Billet billet : billets) {
            assertEquals(StatutBillet.VALIDE, billet.getStatut());
            assertSame(ligne, billet.getLigneReservation());
        }
    }

    @Test
    @DisplayName("genererBillets : codes are unique and start with TKT-")
    void genererBillets_codesUniques() {
        Reservation reservation = reservationAvecLignes(50);
        when(billetRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Billet> billets = ticketService.genererBillets(reservation);

        Set<String> codes = new HashSet<>();
        for (Billet billet : billets) {
            assertTrue(billet.getCode().matches("TKT-[A-F0-9]{16}"), "bad code format: " + billet.getCode());
            codes.add(billet.getCode());
        }
        assertEquals(50, codes.size());
    }

    @Test
    @DisplayName("genererBillets : everything is saved in one saveAll call")
    @SuppressWarnings("unchecked")
    void genererBillets_sauvegardeEnUneFois() {
        Reservation reservation = reservationAvecLignes(1, 1);
        when(billetRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        ticketService.genererBillets(reservation);

        ArgumentCaptor<List<Billet>> captor = ArgumentCaptor.forClass(List.class);
        verify(billetRepository, times(1)).saveAll(captor.capture());
        assertEquals(2, captor.getValue().size());
    }

    @Test
    @DisplayName("genererBillets : a reservation without line creates no ticket")
    void genererBillets_sansLigne() {
        Reservation reservation = reservationAvecLignes();
        when(billetRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        assertTrue(ticketService.genererBillets(reservation).isEmpty());
    }

    // ---------- findByReservation ----------
    @Test
    @DisplayName("findByReservation : unknown reservation -> ResourceNotFoundException")
    void findByReservation_introuvable() {
        when(reservationRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> ticketService.findByReservation(99L));

        verify(billetRepository, never()).findByLigneReservationReservationIdOrderById(anyLong());
    }

    @Test
    @DisplayName("findByReservation : returns the mapped tickets")
    void findByReservation_ok() {
        List<Billet> billets = List.of(billet(StatutBillet.VALIDE));
        List<TicketResponseDTO> reponse = List.of(mock(TicketResponseDTO.class));
        when(reservationRepository.existsById(1L)).thenReturn(true);
        when(billetRepository.findByLigneReservationReservationIdOrderById(1L)).thenReturn(billets);
        when(ticketMapper.toResponseList(billets)).thenReturn(reponse);

        assertSame(reponse, ticketService.findByReservation(1L));
    }

    // ---------- annulerBillets ----------
    @Test
    @DisplayName("annulerBillets : every ticket of every line becomes ANNULE")
    void annulerBillets_tousAnnules() {
        Billet b1 = billet(StatutBillet.VALIDE);
        Billet b2 = billet(StatutBillet.VALIDE);
        Billet b3 = billet(StatutBillet.VALIDE);

        Reservation reservation = reservationAvecLignes(2, 1);
        reservation.getLignes().get(0).setBillets(new ArrayList<>(List.of(b1, b2)));
        reservation.getLignes().get(1).setBillets(new ArrayList<>(List.of(b3)));

        ticketService.annulerBillets(reservation);

        assertEquals(StatutBillet.ANNULE, b1.getStatut());
        assertEquals(StatutBillet.ANNULE, b2.getStatut());
        assertEquals(StatutBillet.ANNULE, b3.getStatut());
    }
}
