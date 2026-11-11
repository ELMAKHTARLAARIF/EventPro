package com.eventpro.service;

import com.eventpro.dto.reservation.LigneReservationRequestDTO;
import com.eventpro.dto.reservation.ReservationRequestDTO;
import com.eventpro.dto.reservation.ReservationUpdateDTO;
import com.eventpro.entity.*;
import com.eventpro.enums.StatutEvenement;
import com.eventpro.enums.StatutReservation;
import com.eventpro.enums.TypeReduction;
import com.eventpro.mapper.ReservationMapper;
import com.eventpro.repository.ClientRepository;
import com.eventpro.repository.EvenementRepository;
import com.eventpro.repository.PromotionRepository;
import com.eventpro.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    ReservationRepository reservationRepository;
    @Mock
    EvenementRepository evenementRepository;
    @Mock
    ClientRepository clientRepository;
    @Mock
    PromotionRepository promotionRepository;
    @Mock
    CategorieBilletService categorieService;
    @Mock
    PaymentService paymentService;
    @Mock
    TicketService ticketService;
    @Mock
    ReservationMapper reservationMapper;

    @InjectMocks
    ReservationService reservationService;

    private Evenement evenement;
    private Client client;
    private CategorieBillet vip;        // 100 MAD, quota 4
    private CategorieBillet standard;   // 50 MAD, quota 6

    @BeforeEach
    void setUp() {
        evenement = new Evenement();
        evenement.setId(1L);
        evenement.setTitre("Gnaoua Festival");
        evenement.setStatut(StatutEvenement.PUBLIE);
        evenement.setDateOuvertureVente(LocalDateTime.now().minusDays(1));
        evenement.setDateClotureVente(LocalDateTime.now().plusDays(30));

        client = new Client();
        client.setId(2L);
        client.setActif(true);

        vip = categorie(1L, "VIP", "100.00", 4);
        standard = categorie(2L, "Standard", "50.00", 6);
    }


    @Test
    void create_valide_construitLesLignes_calculeLeTotal_etConfirme() {
        evenementEtClientTrouves();
        categoriesVerrouillees();
        aucuneReservationAnterieure(0L);
        sauvegardeRenvoieLArgument();

        reservationService.create(new ReservationRequestDTO(1L, 2L, List.of(ligne(1L, 3), ligne(2L, 2)), null));

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        Reservation r = captor.getValue();

        assertEquals(2, r.getLignes().size());
        assertMontant("300.00", r.getLignes().get(0).getSousTotal());   // 3 x 100
        assertMontant("100.00", r.getLignes().get(1).getSousTotal());   // 2 x 50
        assertMontant("400.00", r.getMontantTotal());
        assertEquals(2L, r.getClient().getId());
        assertEquals(1L, r.getEvenement().getId());
        assertNotNull(r.getReference());
        assertTrue(r.getReference().startsWith("RES-"));
        assertEquals(10, Duration.between(r.getDateCreation(), r.getDateExpiration()).toMinutes());
        assertEquals(StatutReservation.CONFIRMEE, r.getStatut());

        // stock, payment and tickets are delegated to the other services
        verify(categorieService).reserverPlaces(vip, 3);
        verify(categorieService).reserverPlaces(standard, 2);
        verify(paymentService).creerPaiement(r);
        verify(categorieService).confirmerVente(vip, 3);
        verify(categorieService).confirmerVente(standard, 2);
        verify(ticketService).genererBillets(r);
    }

    @Test
    void create_avecPromotionPourcentage_appliqueLaRemise() {
        evenementEtClientTrouves();
        categoriesVerrouillees();
        aucuneReservationAnterieure(0L);
        sauvegardeRenvoieLArgument();
        Promotion promo = promotion(TypeReduction.POURCENTAGE, "20", 37, 100);
        when(promotionRepository.findWithLockByCodeIgnoreCaseAndEvenementId("SUMMER20", 1L)).thenReturn(Optional.of(promo));

        reservationService.create(new ReservationRequestDTO(1L, 2L, List.of(ligne(1L, 3), ligne(2L, 2)), "SUMMER20"));

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        Reservation r = captor.getValue();

        assertMontant("320.00", r.getMontantTotal());        // 400 - 20% = 320
        assertSame(promo, r.getPromotion());
        assertEquals(38, promo.getNombreUtilisations());     // one more use counted
    }

    @Test
    void create_avecMontantFixe_neDepassePasLeSousTotal() {
        evenementEtClientTrouves();          //When the service searches for event ID 1, return the event/When the service searches for client ID 2, return the client.
        categoriesVerrouillees();      //The helper probably configures the category service to return these categories when the reservation service requests them.
        aucuneReservationAnterieure(0L);   //This prepares the repository to indicate that there are no previous reservations to consider.
        sauvegardeRenvoieLArgument(); //This helper configures the mock repository's save() method to return the same reservation object it receives.
        Promotion promo = promotion(TypeReduction.MONTANT_FIXE, "100", 0, 100);
        when(promotionRepository.findWithLockByCodeIgnoreCaseAndEvenementId("SUMMER20", 1L)).thenReturn(Optional.of(promo));

        reservationService.create(new ReservationRequestDTO(1L, 2L, List.of(ligne(2L, 1)), "SUMMER20"));

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());

        assertMontant("0.00", captor.getValue().getMontantTotal());   // 50 - min(100, 50) = 0
    }

    @Test
    void create_evenementInconnu_404() {
        when(evenementRepository.findById(1L)).thenReturn(Optional.empty());

        assertErreur(404, () -> reservationService.create(requete(ligne(1L, 1))));

        verifyNoInteractions(categorieService, paymentService, ticketService);
    }

    @Test
    void create_evenementNonPublie_409() {
        evenement.setStatut(StatutEvenement.BROUILLON);
        when(evenementRepository.findById(1L)).thenReturn(Optional.of(evenement));

        assertErreur(409, () -> reservationService.create(requete(ligne(1L, 1))));

        verifyNoInteractions(categorieService, paymentService, ticketService);
    }

    @Test
    void create_ventesFermees_409() {
        evenement.setDateClotureVente(LocalDateTime.now().minusDays(1));
        when(evenementRepository.findById(1L)).thenReturn(Optional.of(evenement));

        assertErreur(409, () -> reservationService.create(requete(ligne(1L, 1))));
    }

    @Test
    void create_clientInconnu_404() {
        when(evenementRepository.findById(1L)).thenReturn(Optional.of(evenement));
        when(clientRepository.findById(2L)).thenReturn(Optional.empty());

        assertErreur(404, () -> reservationService.create(requete(ligne(1L, 1))));
    }

    @Test
    void create_clientDesactive_403() {
        client.setActif(false);
        evenementEtClientTrouves();

        assertErreur(403, () -> reservationService.create(requete(ligne(1L, 1))));

        verifyNoInteractions(categorieService);
    }

    @Test
    void create_categorieDUnAutreEvenement_404() {
        evenementEtClientTrouves();
        // the locked query only returns the categories of THIS event: category 2 is missing
        when(categorieService.verrouiller(anyCollection(), eq(1L))).thenReturn(Map.of(1L, vip));

        assertErreur(404, () -> reservationService.create(requete(ligne(2L, 1))));

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void create_quotaDepasse_400_etAucunePlaceBloquee() {
        evenementEtClientTrouves();
        categoriesVerrouillees();
        // the client already holds 2 VIP, asks 3 more: 5 > quota of 4
        when(reservationRepository.quantiteDejaReservee(eq(2L), eq(1L), anyCollection(), eq(0L))).thenReturn(2L);

        assertErreur(400, () -> reservationService.create(requete(ligne(1L, 3))));

        verify(categorieService, never()).reserverPlaces(any(), anyInt());
        verify(reservationRepository, never()).save(any());
        verifyNoInteractions(paymentService, ticketService);
    }

    @Test
    void create_codePromoInconnu_404_etRienNEstSauvegarde() {
        evenementEtClientTrouves();
        categoriesVerrouillees();
        aucuneReservationAnterieure(0L);
        when(promotionRepository.findWithLockByCodeIgnoreCaseAndEvenementId("NOPE", 1L)).thenReturn(Optional.empty());

        assertErreur(404, () -> reservationService.create(new ReservationRequestDTO(1L, 2L, List.of(ligne(1L, 1)), "NOPE")));

        verify(reservationRepository, never()).save(any());
        verifyNoInteractions(paymentService, ticketService);
    }

    @Test
    void create_codePromoEpuise_409() {
        evenementEtClientTrouves();
        categoriesVerrouillees();
        aucuneReservationAnterieure(0L);
        Promotion epuisee = promotion(TypeReduction.POURCENTAGE, "20", 100, 100);   // 100 / 100 used
        when(promotionRepository.findWithLockByCodeIgnoreCaseAndEvenementId("SUMMER20", 1L)).thenReturn(Optional.of(epuisee));

        assertErreur(409, () -> reservationService.create(new ReservationRequestDTO(1L, 2L, List.of(ligne(1L, 1)), "SUMMER20")));

        assertEquals(100, epuisee.getNombreUtilisations());   // not incremented
        verify(reservationRepository, never()).save(any());
    }

    // =====================================================================
    // UPDATE (only a pending reservation can be changed)
    // =====================================================================

    @Test
    void update_valide_remplaceLesLignes_sansProlongerLeDelai() {
        Reservation reservation = reservationEnAttente();      // VIP x3 = 300
        LocalDateTime expirationAvant = reservation.getDateExpiration();
        when(reservationRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(reservation));
        categoriesVerrouillees();
        aucuneReservationAnterieure(10L);   // the reservation excludes ITSELF from the quota

        reservationService.update(10L, new ReservationUpdateDTO(List.of(ligne(1L, 2), ligne(2L, 1)), null));

        verify(categorieService).libererPlaces(vip, 3);        // old line released
        verify(categorieService).reserverPlaces(vip, 2);       // new lines blocked
        verify(categorieService).reserverPlaces(standard, 1);

        assertEquals(2, reservation.getLignes().size());
        assertMontant("250.00", reservation.getMontantTotal()); // 2 x 100 + 1 x 50
        assertEquals(StatutReservation.EN_ATTENTE, reservation.getStatut());
        assertEquals(expirationAvant, reservation.getDateExpiration());   // NOT extended
        assertEquals("RES-2026-TEST", reservation.getReference());
        verifyNoInteractions(paymentService, ticketService);
    }

    @Test
    void update_changeDeCategorie_verrouilleAncienneEtNouvelleCategorie() {
        Reservation reservation = reservationEnAttente();      // VIP x3
        when(reservationRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(reservation));
        categoriesVerrouillees();
        aucuneReservationAnterieure(10L);

        reservationService.update(10L, new ReservationUpdateDTO(List.of(ligne(2L, 2)), null));   // Standard only

        // old category (1) AND new category (2) are locked together
        verify(categorieService).verrouiller(argThat(ids -> ids.containsAll(List.of(1L, 2L))), eq(1L));
        verify(categorieService).libererPlaces(vip, 3);
        verify(categorieService).reserverPlaces(standard, 2);
        assertEquals(1, reservation.getLignes().size());
        assertMontant("100.00", reservation.getMontantTotal());
    }

    @Test
    void update_sansCodePromo_retireLaPromotion_etRendUneUtilisation() {
        Reservation reservation = reservationEnAttente();
        Promotion promo = promotion(TypeReduction.POURCENTAGE, "20", 38, 100);
        reservation.setPromotion(promo);
        when(reservationRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(reservation));
        categoriesVerrouillees();
        aucuneReservationAnterieure(10L);
        when(promotionRepository.findWithLockByCodeIgnoreCaseAndEvenementId("SUMMER20", 1L)).thenReturn(Optional.of(promo));

        reservationService.update(10L, new ReservationUpdateDTO(List.of(ligne(1L, 3)), null));

        assertNull(reservation.getPromotion());
        assertEquals(37, promo.getNombreUtilisations());        // the use was given back
        assertMontant("300.00", reservation.getMontantTotal()); // full price again
    }

    @Test
    void update_reservationInconnue_404() {
        when(reservationRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertErreur(404, () -> reservationService.update(99L, new ReservationUpdateDTO(List.of(ligne(1L, 1)), null)));
    }

    @Test
    void update_reservationDejaConfirmee_409() {
        Reservation reservation = reservationEnAttente();
        reservation.setStatut(StatutReservation.CONFIRMEE);
        when(reservationRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(reservation));

        assertErreur(409, () -> reservationService.update(10L, new ReservationUpdateDTO(List.of(ligne(1L, 1)), null)));

        verifyNoInteractions(categorieService);
    }

    @Test
    void update_reservationExpiree_409() {
        Reservation reservation = reservationEnAttente();
        reservation.setDateExpiration(LocalDateTime.now().minusMinutes(1));
        when(reservationRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(reservation));

        assertErreur(409, () -> reservationService.update(10L, new ReservationUpdateDTO(List.of(ligne(1L, 1)), null)));

        verifyNoInteractions(categorieService);
    }

    @Test
    void update_quotaDepasse_400() {
        Reservation reservation = reservationEnAttente();
        when(reservationRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(reservation));
        categoriesVerrouillees();
        // the client holds 3 VIP in OTHER reservations, asks 2 more: 5 > quota of 4
        when(reservationRepository.quantiteDejaReservee(eq(2L), eq(1L), anyCollection(), eq(10L))).thenReturn(3L);

        assertErreur(400, () -> reservationService.update(10L, new ReservationUpdateDTO(List.of(ligne(1L, 2)), null)));

        verify(categorieService, never()).reserverPlaces(any(), anyInt());
        // in production the @Transactional rollback restores the released places
    }

    // helpers

    private void evenementEtClientTrouves() {
        when(evenementRepository.findById(1L)).thenReturn(Optional.of(evenement));
        when(clientRepository.findById(2L)).thenReturn(Optional.of(client));
    }

    private void categoriesVerrouillees() {
        when(categorieService.verrouiller(anyCollection(), eq(1L))).thenReturn(Map.of(1L, vip, 2L, standard));
    }

    // "exclure" = id of the reservation ignored by the quota query (0 when creating)
    private void aucuneReservationAnterieure(Long exclure) {
        when(reservationRepository.quantiteDejaReservee(eq(2L), anyLong(), anyCollection(), eq(exclure))).thenReturn(0L);
    }

    private void sauvegardeRenvoieLArgument() {
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ReservationRequestDTO requete(LigneReservationRequestDTO... lignes) {
        return new ReservationRequestDTO(1L, 2L, List.of(lignes), null);
    }

    private LigneReservationRequestDTO ligne(Long categorieId, int quantite) {
        return new LigneReservationRequestDTO(categorieId, quantite);
    }

    private CategorieBillet categorie(Long id, String nom, String prix, int quota) {
        CategorieBillet c = new CategorieBillet();
        c.setId(id);
        c.setNom(nom);
        c.setPrixUnitaire(new BigDecimal(prix));
        c.setQuantiteTotal(100);
        c.setQuotaMaxParClient(quota);
        c.setEvenement(evenement);
        return c;
    }

    private Promotion promotion(TypeReduction type, String valeur, int utilisations, int limite) {
        Promotion p = new Promotion();
        p.setId(5L);
        p.setCode("SUMMER20");
        p.setTypeReduction(type);
        p.setValeurReduction(new BigDecimal(valeur));
        p.setDateDebut(LocalDate.now().minusDays(1));
        p.setDateFin(LocalDate.now().plusDays(30));
        p.setNombreUtilisations(utilisations);
        p.setLimiteUtilisation(limite);
        p.setEvenement(evenement);
        return p;
    }

    // a reservation still waiting for payment: VIP x3, expires in 8 minutes
    private Reservation reservationEnAttente() {
        Reservation r = new Reservation();
        r.setId(10L);
        r.setReference("RES-2026-TEST");
        r.setClient(client);
        r.setEvenement(evenement);
        r.setStatut(StatutReservation.EN_ATTENTE);
        r.setDateCreation(LocalDateTime.now().minusMinutes(2));
        r.setDateExpiration(LocalDateTime.now().plusMinutes(8));

        LigneReservation ancienne = new LigneReservation();
        ancienne.setCategorie(vip);
        ancienne.setQuantite(3);
        ancienne.setPrixUnitaire(vip.getPrixUnitaire());
        ancienne.setSousTotal(new BigDecimal("300.00"));
        r.addLigne(ancienne);

        r.setMontantTotal(new BigDecimal("300.00"));
        return r;
    }

    private void assertMontant(String attendu, BigDecimal reel) {
        assertEquals(0, new BigDecimal(attendu).compareTo(reel), "montant attendu " + attendu + " mais obtenu " + reel);
    }

    private void assertErreur(int statutAttendu, Executable action) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, action);
        assertEquals(statutAttendu, ex.getStatusCode().value());
    }
}