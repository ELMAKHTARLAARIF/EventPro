package com.eventpro.service;

import com.eventpro.dto.CheckInResponseDTO;
import com.eventpro.dto.egent.CheckInRequestDTO;
import com.eventpro.entity.AgentControle;
import com.eventpro.entity.Billet;
import com.eventpro.entity.Evenement;
import com.eventpro.entity.LigneReservation;
import com.eventpro.entity.Reservation;
import com.eventpro.enums.ResultatControle;
import com.eventpro.enums.StatutBillet;
import com.eventpro.mapper.CheckInMapper;
import com.eventpro.repository.AgentControleRepository;
import com.eventpro.repository.BilletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckInServiceTest {

    private static final Long EVENT_ID = 1L;
    private static final Long AGENT_ID = 7L;
    private static final String CODE = "TKT-ABCDEF1234567890";

    @Mock private BilletRepository billetRepository;
    @Mock private AgentControleRepository agentRepository;
    @Mock private CheckInMapper checkInMapper;

    @InjectMocks private CheckInService checkInService;

    private final CheckInResponseDTO reponse = mock(CheckInResponseDTO.class);

    @BeforeEach
    void setUp() {
        // the mapper just returns a fake response: we check which ResultatControle was given to it
        lenient().when(checkInMapper.toResponse(any(), any(), any(), any())).thenReturn(reponse);
    }

    // ---------- helpers ----------
    private CheckInRequestDTO requete(String code, Long agentId, Long evenementId) {
        CheckInRequestDTO dto = mock(CheckInRequestDTO.class);
        lenient().when(dto.code()).thenReturn(code);
        lenient().when(dto.agentId()).thenReturn(agentId);
        lenient().when(dto.evenementId()).thenReturn(evenementId);
        return dto;
    }

    private AgentControle agent(boolean actif) {
        AgentControle agent = new AgentControle();
        agent.setId(AGENT_ID);
        agent.setActif(actif);
        return agent;
    }

    private Billet billet(StatutBillet statut, Long evenementId) {
        Evenement evenement = new Evenement();
        evenement.setId(evenementId);
        Reservation reservation = new Reservation();
        reservation.setEvenement(evenement);
        LigneReservation ligne = new LigneReservation();
        ligne.setReservation(reservation);

        Billet billet = new Billet();
        billet.setCode(CODE);
        billet.setStatut(statut);
        billet.setLigneReservation(ligne);
        return billet;
    }

    private ResultatControle dernierResultat(int fois) {
        ArgumentCaptor<ResultatControle> captor = ArgumentCaptor.forClass(ResultatControle.class);
        verify(checkInMapper, times(fois)).toResponse(any(), captor.capture(), any(), any());
        return captor.getValue();
    }

    private void agentActifEtBillet(Billet billet) {
        when(agentRepository.findById(AGENT_ID)).thenReturn(Optional.of(agent(true)));
        when(billetRepository.findByCodeForUpdate(CODE)).thenReturn(Optional.of(billet));
    }

    // =====================================================
    //                       scanner
    // =====================================================
    @Test
    @DisplayName("scanner : unknown agent -> 404")
    void scanner_agentIntrouvable() {
        when(agentRepository.findById(AGENT_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> checkInService.scanner(requete(CODE, AGENT_ID, EVENT_ID)));

        assertEquals(404, ex.getStatusCode().value());
        verifyNoInteractions(billetRepository);
    }

    @Test
    @DisplayName("scanner : deactivated agent -> 403, the ticket is not touched")
    void scanner_agentDesactive() {
        when(agentRepository.findById(AGENT_ID)).thenReturn(Optional.of(agent(false)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> checkInService.scanner(requete(CODE, AGENT_ID, EVENT_ID)));

        assertEquals(403, ex.getStatusCode().value());
        verifyNoInteractions(billetRepository);
    }

    @Test
    @DisplayName("scanner : unknown ticket -> 404")
    void scanner_billetIntrouvable() {
        when(agentRepository.findById(AGENT_ID)).thenReturn(Optional.of(agent(true)));
        when(billetRepository.findByCodeForUpdate(CODE)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> checkInService.scanner(requete(CODE, AGENT_ID, EVENT_ID)));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("scanner : the code is trimmed and uppercased before the search")
    void scanner_normaliseLeCode() {
        Billet billet = billet(StatutBillet.VALIDE, EVENT_ID);
        when(agentRepository.findById(AGENT_ID)).thenReturn(Optional.of(agent(true)));
        when(billetRepository.findByCodeForUpdate(CODE)).thenReturn(Optional.of(billet));

        checkInService.scanner(requete("  " + CODE.toLowerCase() + "  ", AGENT_ID, EVENT_ID));

        verify(billetRepository).findByCodeForUpdate(CODE);
    }

    @Test
    @DisplayName("scanner : valid ticket -> VALIDE, ticket becomes UTILISE with a date")
    void scanner_billetValide() {
        Billet billet = billet(StatutBillet.VALIDE, EVENT_ID);
        agentActifEtBillet(billet);

        CheckInResponseDTO resultat = checkInService.scanner(requete(CODE, AGENT_ID, EVENT_ID));

        assertSame(reponse, resultat);
        assertEquals(ResultatControle.VALIDE, dernierResultat(1));
        assertEquals(StatutBillet.UTILISE, billet.getStatut());
        assertNotNull(billet.getDateControle());
        assertFalse(billet.getDateControle().isAfter(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    @DisplayName("scanner : second scan of the same ticket -> DEJA_UTILISE (no double check-in)")
    void scanner_doublePassage() {
        Billet billet = billet(StatutBillet.VALIDE, EVENT_ID);
        agentActifEtBillet(billet);
        CheckInRequestDTO requete = requete(CODE, AGENT_ID, EVENT_ID);

        checkInService.scanner(requete);                 // first scan: accepted
        LocalDateTime premierPassage = billet.getDateControle();
        checkInService.scanner(requete);                 // second scan: refused

        assertEquals(ResultatControle.DEJA_UTILISE, dernierResultat(2));
        assertEquals(StatutBillet.UTILISE, billet.getStatut());
        assertEquals(premierPassage, billet.getDateControle());   // date of the first passage is kept
    }

    @Test
    @DisplayName("scanner : ticket of another event -> INVALIDE and it stays VALIDE")
    void scanner_autreEvenement() {
        Billet billet = billet(StatutBillet.VALIDE, 999L);
        agentActifEtBillet(billet);

        checkInService.scanner(requete(CODE, AGENT_ID, EVENT_ID));

        assertEquals(ResultatControle.INVALIDE, dernierResultat(1));
        assertEquals(StatutBillet.VALIDE, billet.getStatut());
        assertNull(billet.getDateControle());
    }

    @Test
    @DisplayName("scanner : cancelled / refunded ticket -> INVALIDE")
    void scanner_billetAnnule() {
        Billet billet = billet(StatutBillet.ANNULE, EVENT_ID);
        agentActifEtBillet(billet);

        checkInService.scanner(requete(CODE, AGENT_ID, EVENT_ID));

        assertEquals(ResultatControle.INVALIDE, dernierResultat(1));
        assertEquals(StatutBillet.ANNULE, billet.getStatut());
    }

    // =====================================================
    //                       verifier
    // =====================================================
    @Test
    @DisplayName("verifier : unknown ticket -> 404")
    void verifier_introuvable() {
        when(billetRepository.findByCode(CODE)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> checkInService.verifier(CODE));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("verifier : valid ticket -> VALIDE and nothing is composted")
    void verifier_valide_neChangeRien() {
        Billet billet = billet(StatutBillet.VALIDE, EVENT_ID);
        when(billetRepository.findByCode(CODE)).thenReturn(Optional.of(billet));

        checkInService.verifier(CODE);

        assertEquals(ResultatControle.VALIDE, dernierResultat(1));
        assertEquals(StatutBillet.VALIDE, billet.getStatut());
        assertNull(billet.getDateControle());
    }

    @Test
    @DisplayName("verifier : already used ticket -> DEJA_UTILISE")
    void verifier_dejaUtilise() {
        Billet billet = billet(StatutBillet.UTILISE, EVENT_ID);
        when(billetRepository.findByCode(CODE)).thenReturn(Optional.of(billet));

        checkInService.verifier(CODE);

        assertEquals(ResultatControle.DEJA_UTILISE, dernierResultat(1));
    }

    @Test
    @DisplayName("verifier : cancelled ticket -> INVALIDE")
    void verifier_annule() {
        Billet billet = billet(StatutBillet.ANNULE, EVENT_ID);
        when(billetRepository.findByCode(CODE)).thenReturn(Optional.of(billet));

        checkInService.verifier(CODE);

        assertEquals(ResultatControle.INVALIDE, dernierResultat(1));
    }

    @Test
    @DisplayName("verifier : the code is trimmed and uppercased")
    void verifier_normaliseLeCode() {
        Billet billet = billet(StatutBillet.VALIDE, EVENT_ID);
        when(billetRepository.findByCode(CODE)).thenReturn(Optional.of(billet));

        checkInService.verifier("  " + CODE.toLowerCase() + " ");

        verify(billetRepository).findByCode(CODE);
    }
}
