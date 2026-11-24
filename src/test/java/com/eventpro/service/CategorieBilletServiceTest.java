package com.eventpro.service;

import com.eventpro.dto.category_billet.CategorieBilletRequestDTO;
import com.eventpro.dto.category_billet.CategorieBilletResponseDTO;
import com.eventpro.entity.CategorieBillet;
import com.eventpro.entity.Evenement;
import com.eventpro.enums.StatutEvenement;
import com.eventpro.exception.InsufficientStockException;
import com.eventpro.exception.ResourceNotFoundException;
import com.eventpro.exception.ValidationException;
import com.eventpro.mapper.CategorieBilletMapper;
import com.eventpro.repository.CategorieBilletRepository;
import com.eventpro.repository.EvenementRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategorieBilletServiceTest {

    private static final Long EVENT_ID = 1L;
    private static final Long CAT_ID = 5L;

    @Mock private CategorieBilletRepository categorieBilletRepository;
    @Mock private EvenementRepository evenementRepository;
    @Mock private CategorieBilletMapper categorieMapper;

    @InjectMocks private CategorieBilletService service;

    // ---------- helpers ----------
    private Evenement evenement(StatutEvenement statut, int capacite) {
        Evenement evenement = new Evenement();
        evenement.setId(EVENT_ID);
        evenement.setStatut(statut);
        evenement.setCapacite(capacite);
        return evenement;
    }

    private CategorieBillet categorie(int total, int reservees, int vendues) {
        CategorieBillet categorie = new CategorieBillet();
        categorie.setId(CAT_ID);
        categorie.setNom("VIP");
        categorie.setQuantiteTotal(total);
        categorie.setPlacesReservees(reservees);
        categorie.setPlacesVendues(vendues);
        return categorie;
    }

    private CategorieBilletRequestDTO requete(String nom, int quantiteTotal) {
        CategorieBilletRequestDTO dto = mock(CategorieBilletRequestDTO.class);
        lenient().when(dto.nom()).thenReturn(nom);
        lenient().when(dto.quantiteTotal()).thenReturn(quantiteTotal);
        return dto;
    }

    // =====================================================
    //                        create
    // =====================================================
    @Test
    @DisplayName("create : valid category is linked to the event and saved")
    void create_ok() {
        Evenement evenement = evenement(StatutEvenement.BROUILLON, 100);
        CategorieBillet categorie = categorie(50, 0, 0);
        CategorieBilletResponseDTO reponse = mock(CategorieBilletResponseDTO.class);
        CategorieBilletRequestDTO dto = requete(" VIP ", 50);

        when(evenementRepository.findById(EVENT_ID)).thenReturn(Optional.of(evenement));
        when(categorieBilletRepository.existsByNomIgnoreCaseAndEvenementId("VIP", EVENT_ID)).thenReturn(false);
        when(categorieBilletRepository.totalQuantite(EVENT_ID)).thenReturn(30L);
        when(categorieMapper.toEntity(dto)).thenReturn(categorie);
        when(categorieBilletRepository.save(categorie)).thenReturn(categorie);
        when(categorieMapper.toResponse(categorie)).thenReturn(reponse);

        assertSame(reponse, service.create(EVENT_ID, dto));
        assertSame(evenement, categorie.getEvenement());
        verify(categorieBilletRepository).save(categorie);
    }

    @Test
    @DisplayName("create : total tickets exactly equal to the capacity is accepted")
    void create_capaciteExacte() {
        Evenement evenement = evenement(StatutEvenement.BROUILLON, 100);
        CategorieBillet categorie = categorie(50, 0, 0);
        CategorieBilletRequestDTO dto = requete("VIP", 50);

        when(evenementRepository.findById(EVENT_ID)).thenReturn(Optional.of(evenement));
        when(categorieBilletRepository.existsByNomIgnoreCaseAndEvenementId("VIP", EVENT_ID)).thenReturn(false);
        when(categorieBilletRepository.totalQuantite(EVENT_ID)).thenReturn(50L);   // 50 + 50 = 100
        when(categorieMapper.toEntity(dto)).thenReturn(categorie);
        when(categorieBilletRepository.save(categorie)).thenReturn(categorie);

        assertDoesNotThrow(() -> service.create(EVENT_ID, dto));
    }

    @Test
    @DisplayName("create : unknown event -> ResourceNotFoundException")
    void create_evenementIntrouvable() {
        when(evenementRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.create(EVENT_ID, requete("VIP", 10)));
    }

    @Test
    @DisplayName("create : cancelled or finished event -> 409")
    void create_evenementAnnuleOuTermine() {
        for (StatutEvenement statut : List.of(StatutEvenement.ANNULE, StatutEvenement.TERMINE)) {
            when(evenementRepository.findById(EVENT_ID)).thenReturn(Optional.of(evenement(statut, 100)));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.create(EVENT_ID, requete("VIP", 10)));

            assertEquals(409, ex.getStatusCode().value());
        }
        verify(categorieBilletRepository, never()).save(any());
    }

    @Test
    @DisplayName("create : duplicate category name -> 409")
    void create_nomDejaPris() {
        when(evenementRepository.findById(EVENT_ID)).thenReturn(Optional.of(evenement(StatutEvenement.BROUILLON, 100)));
        when(categorieBilletRepository.existsByNomIgnoreCaseAndEvenementId("VIP", EVENT_ID)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(EVENT_ID, requete("VIP", 10)));

        assertEquals(409, ex.getStatusCode().value());
        verify(categorieBilletRepository, never()).save(any());
    }

    @Test
    @DisplayName("create : total tickets above the event capacity -> ValidationException")
    void create_capaciteDepassee() {
        when(evenementRepository.findById(EVENT_ID)).thenReturn(Optional.of(evenement(StatutEvenement.BROUILLON, 100)));
        when(categorieBilletRepository.existsByNomIgnoreCaseAndEvenementId("VIP", EVENT_ID)).thenReturn(false);
        when(categorieBilletRepository.totalQuantite(EVENT_ID)).thenReturn(80L);    // 80 + 50 = 130 > 100

        assertThrows(ValidationException.class, () -> service.create(EVENT_ID, requete("VIP", 50)));

        verify(categorieBilletRepository, never()).save(any());
    }

    // =====================================================
    //                         read
    // =====================================================
    @Test
    @DisplayName("findByEvenement : unknown event -> ResourceNotFoundException")
    void findByEvenement_introuvable() {
        when(evenementRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findByEvenement(EVENT_ID));
    }

    @Test
    @DisplayName("findByEvenement : returns the mapped categories")
    void findByEvenement_ok() {
        List<CategorieBillet> categories = List.of(categorie(10, 0, 0));
        List<CategorieBilletResponseDTO> reponse = List.of(mock(CategorieBilletResponseDTO.class));
        when(evenementRepository.findById(EVENT_ID)).thenReturn(Optional.of(evenement(StatutEvenement.PUBLIE, 100)));
        when(categorieBilletRepository.findByEvenementId(EVENT_ID)).thenReturn(categories);
        when(categorieMapper.toResponseList(categories)).thenReturn(reponse);

        assertSame(reponse, service.findByEvenement(EVENT_ID));
    }

    @Test
    @DisplayName("findOne : category not in this event -> ResourceNotFoundException")
    void findOne_introuvable() {
        when(categorieBilletRepository.findByIdAndEvenementId(CAT_ID, EVENT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findOne(EVENT_ID, CAT_ID));
    }

    @Test
    @DisplayName("findOne : returns the mapped category")
    void findOne_ok() {
        CategorieBillet categorie = categorie(10, 0, 0);
        CategorieBilletResponseDTO reponse = mock(CategorieBilletResponseDTO.class);
        when(categorieBilletRepository.findByIdAndEvenementId(CAT_ID, EVENT_ID)).thenReturn(Optional.of(categorie));
        when(categorieMapper.toResponse(categorie)).thenReturn(reponse);

        assertSame(reponse, service.findOne(EVENT_ID, CAT_ID));
    }

    // =====================================================
    //                        update
    // =====================================================
    private CategorieBillet categorieLieeALevenement(int total, int reservees, int vendues, int capacite) {
        CategorieBillet categorie = categorie(total, reservees, vendues);
        categorie.setEvenement(evenement(StatutEvenement.BROUILLON, capacite));
        when(categorieBilletRepository.findByIdAndEvenementId(CAT_ID, EVENT_ID)).thenReturn(Optional.of(categorie));
        return categorie;
    }

    @Test
    @DisplayName("update : valid change fills the managed entity")
    void update_ok() {
        CategorieBillet categorie = categorieLieeALevenement(100, 10, 5, 200);
        CategorieBilletRequestDTO dto = requete("Gold", 50);
        CategorieBilletResponseDTO reponse = mock(CategorieBilletResponseDTO.class);

        when(categorieBilletRepository.existsByNomIgnoreCaseAndEvenementIdAndIdNot("Gold", EVENT_ID, CAT_ID)).thenReturn(false);
        when(categorieBilletRepository.totalQuantite(EVENT_ID)).thenReturn(100L);   // others = 100 - 100 = 0
        when(categorieMapper.toResponse(categorie)).thenReturn(reponse);

        assertSame(reponse, service.update(EVENT_ID, CAT_ID, dto));
        verify(categorieMapper).updateEntity(dto, categorie);
    }

    @Test
    @DisplayName("update : unknown category -> ResourceNotFoundException")
    void update_introuvable() {
        when(categorieBilletRepository.findByIdAndEvenementId(CAT_ID, EVENT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(EVENT_ID, CAT_ID, requete("Gold", 50)));
    }

    @Test
    @DisplayName("update : new name already used by another category -> 409")
    void update_nomDejaPris() {
        categorieLieeALevenement(100, 0, 0, 200);
        when(categorieBilletRepository.existsByNomIgnoreCaseAndEvenementIdAndIdNot("Gold", EVENT_ID, CAT_ID)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.update(EVENT_ID, CAT_ID, requete("Gold", 50)));

        assertEquals(409, ex.getStatusCode().value());
        verify(categorieMapper, never()).updateEntity(any(), any());
    }

    @Test
    @DisplayName("update : quantity below reserved + sold places -> ValidationException")
    void update_quantiteInferieureAuxPlacesUtilisees() {
        categorieLieeALevenement(100, 10, 5, 200);          // 15 places already used
        when(categorieBilletRepository.existsByNomIgnoreCaseAndEvenementIdAndIdNot("Gold", EVENT_ID, CAT_ID)).thenReturn(false);

        assertThrows(ValidationException.class, () -> service.update(EVENT_ID, CAT_ID, requete("Gold", 14)));

        verify(categorieMapper, never()).updateEntity(any(), any());
    }

    @Test
    @DisplayName("update : new total above the event capacity -> ValidationException")
    void update_capaciteDepassee() {
        categorieLieeALevenement(100, 0, 0, 150);
        when(categorieBilletRepository.existsByNomIgnoreCaseAndEvenementIdAndIdNot("Gold", EVENT_ID, CAT_ID)).thenReturn(false);
        when(categorieBilletRepository.totalQuantite(EVENT_ID)).thenReturn(140L);   // others = 40, 40 + 120 > 150

        assertThrows(ValidationException.class, () -> service.update(EVENT_ID, CAT_ID, requete("Gold", 120)));
    }

    // =====================================================
    //                        delete
    // =====================================================
    @Test
    @DisplayName("delete : allowed when the event is a draft")
    void delete_brouillon() {
        CategorieBillet categorie = categorieLieeALevenement(10, 0, 0, 100);

        service.delete(EVENT_ID, CAT_ID);

        verify(categorieBilletRepository).delete(categorie);
    }

    @Test
    @DisplayName("delete : refused when the event is not a draft -> 409")
    void delete_evenementPublie() {
        CategorieBillet categorie = categorie(10, 0, 0);
        categorie.setEvenement(evenement(StatutEvenement.PUBLIE, 100));
        when(categorieBilletRepository.findByIdAndEvenementId(CAT_ID, EVENT_ID)).thenReturn(Optional.of(categorie));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.delete(EVENT_ID, CAT_ID));

        assertEquals(409, ex.getStatusCode().value());
        verify(categorieBilletRepository, never()).delete(any(CategorieBillet.class));
    }

    @Test
    @DisplayName("delete : unknown category -> ResourceNotFoundException")
    void delete_introuvable() {
        when(categorieBilletRepository.findByIdAndEvenementId(CAT_ID, EVENT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(EVENT_ID, CAT_ID));
    }

    // =====================================================
    //                 stock (used by reservations)
    // =====================================================
    @Test
    @DisplayName("verrouiller : returns the locked categories indexed by id")
    void verrouiller_indexeParId() {
        CategorieBillet c1 = categorie(10, 0, 0);
        c1.setId(1L);
        CategorieBillet c2 = categorie(10, 0, 0);
        c2.setId(2L);
        List<Long> ids = List.of(1L, 2L);
        when(categorieBilletRepository.findAllForUpdate(ids, EVENT_ID)).thenReturn(List.of(c1, c2));

        Map<Long, CategorieBillet> resultat = service.verrouiller(ids, EVENT_ID);

        assertEquals(2, resultat.size());
        assertSame(c1, resultat.get(1L));
        assertSame(c2, resultat.get(2L));
    }

    @Test
    @DisplayName("reserverPlaces : blocks the requested places")
    void reserverPlaces_ok() {
        CategorieBillet categorie = categorie(100, 10, 20);

        service.reserverPlaces(categorie, 30);

        assertEquals(40, categorie.getPlacesReservees());
        assertEquals(20, categorie.getPlacesVendues());
    }

    @Test
    @DisplayName("reserverPlaces : taking exactly the last available places works")
    void reserverPlaces_dernieresPlaces() {
        CategorieBillet categorie = categorie(100, 10, 20);      // 70 available

        service.reserverPlaces(categorie, 70);

        assertEquals(80, categorie.getPlacesReservees());
        assertEquals(0, categorie.placesDisponibles());
    }

    @Test
    @DisplayName("reserverPlaces : quantity above the available places -> InsufficientStockException, counters unchanged")
    void reserverPlaces_stockInsuffisant() {
        CategorieBillet categorie = categorie(100, 10, 20);      // 70 available

        assertThrows(InsufficientStockException.class, () -> service.reserverPlaces(categorie, 71));

        assertEquals(10, categorie.getPlacesReservees());
        assertEquals(20, categorie.getPlacesVendues());
    }

    @Test
    @DisplayName("reserverPlaces : sold-out category rejects any request")
    void reserverPlaces_complet() {
        CategorieBillet categorie = categorie(100, 40, 60);

        assertThrows(InsufficientStockException.class, () -> service.reserverPlaces(categorie, 1));
    }

    @Test
    @DisplayName("libererPlaces : gives the blocked places back")
    void libererPlaces_ok() {
        CategorieBillet categorie = categorie(100, 30, 0);

        service.libererPlaces(categorie, 10);

        assertEquals(20, categorie.getPlacesReservees());
    }

    @Test
    @DisplayName("libererPlaces : never goes below zero")
    void libererPlaces_pasDeNegatif() {
        CategorieBillet categorie = categorie(100, 5, 0);

        service.libererPlaces(categorie, 10);

        assertEquals(0, categorie.getPlacesReservees());
    }

    @Test
    @DisplayName("confirmerVente : reserved places become sold places")
    void confirmerVente_ok() {
        CategorieBillet categorie = categorie(100, 10, 20);

        service.confirmerVente(categorie, 4);

        assertEquals(6, categorie.getPlacesReservees());
        assertEquals(24, categorie.getPlacesVendues());
    }

    @Test
    @DisplayName("confirmerVente : reserved places never go below zero")
    void confirmerVente_reserveesPasNegatif() {
        CategorieBillet categorie = categorie(100, 2, 20);

        service.confirmerVente(categorie, 5);

        assertEquals(0, categorie.getPlacesReservees());
        assertEquals(25, categorie.getPlacesVendues());
    }

    @Test
    @DisplayName("annulerVente : refunded places can be sold again")
    void annulerVente_ok() {
        CategorieBillet categorie = categorie(100, 0, 20);

        service.annulerVente(categorie, 5);

        assertEquals(15, categorie.getPlacesVendues());
        assertEquals(85, categorie.placesDisponibles());
    }

    @Test
    @DisplayName("annulerVente : sold places never go below zero")
    void annulerVente_pasDeNegatif() {
        CategorieBillet categorie = categorie(100, 0, 3);

        service.annulerVente(categorie, 10);

        assertEquals(0, categorie.getPlacesVendues());
    }
}
