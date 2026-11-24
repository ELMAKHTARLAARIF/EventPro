package com.eventpro.service;

import com.eventpro.dto.event.EventRequestDTO;
import com.eventpro.dto.event.EventResponseDTO;
import com.eventpro.entity.CategorieBillet;
import com.eventpro.entity.Evenement;
import com.eventpro.entity.Organisateur;
import com.eventpro.enums.StatutEvenement;
import com.eventpro.exception.ResourceNotFoundException;
import com.eventpro.exception.ValidationException;
import com.eventpro.mapper.EventMapper;
import com.eventpro.repository.EvenementRepository;
import com.eventpro.repository.OrganisateurRepository;
import com.eventpro.repository.PromotionRepository;
import com.eventpro.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock EvenementRepository evenementRepository;
    @Mock OrganisateurRepository organisateurRepository;
    @Mock PromotionRepository promotionRepository;
    @Mock EventMapper eventMapper;
    @Mock RemboursementService remboursementService;
    @Mock UserRepository userRepository;

    @InjectMocks EventService service;

    // ---------- create ----------

    @Test
    void create_sansOrganisateur_validationException() {
        EventRequestDTO dto = mock(EventRequestDTO.class);
        when(dto.organisateurId()).thenReturn(null);

        assertThrows(ValidationException.class, () -> service.create(dto));
        verifyNoInteractions(organisateurRepository);
    }

    @Test
    void create_organisateurInconnu_404() {
        EventRequestDTO dto = mock(EventRequestDTO.class);
        when(dto.organisateurId()).thenReturn(1L);
        when(organisateurRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.create(dto));
    }

    @Test
    void create_dateDebutPassee_validationException() {
        EventRequestDTO dto = mock(EventRequestDTO.class);
        when(dto.organisateurId()).thenReturn(1L);
        when(dto.dateDebut()).thenReturn(LocalDateTime.now().minusDays(1));
        when(organisateurRepository.findById(1L)).thenReturn(Optional.of(mock(Organisateur.class)));

        assertThrows(ValidationException.class, () -> service.create(dto));
        verify(evenementRepository, never()).save(any());
    }

    @Test
    void create_doublon_409() {
        LocalDateTime debut = LocalDateTime.now().plusDays(10);
        EventRequestDTO dto = mock(EventRequestDTO.class);
        Organisateur org = mock(Organisateur.class);
        when(dto.organisateurId()).thenReturn(1L);
        when(dto.dateDebut()).thenReturn(debut);
        when(dto.titre()).thenReturn("Concert");
        when(org.getId()).thenReturn(1L);
        when(organisateurRepository.findById(1L)).thenReturn(Optional.of(org));
        when(evenementRepository.existsByOrganisateurIdAndTitreIgnoreCaseAndDateDebut(1L, "Concert", debut)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(dto));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void create_valide_brouillon_etTexteNettoye() {
        LocalDateTime debut = LocalDateTime.now().plusDays(10);
        EventRequestDTO dto = mock(EventRequestDTO.class);
        Organisateur org = mock(Organisateur.class);
        EventResponseDTO response = mock(EventResponseDTO.class);
        Evenement entite = new Evenement();
        entite.setTitre("  Concert  ");
        entite.setLieu("  Casablanca ");

        when(dto.organisateurId()).thenReturn(1L);
        when(dto.dateDebut()).thenReturn(debut);
        when(dto.titre()).thenReturn("  Concert  ");
        when(org.getId()).thenReturn(1L);
        when(organisateurRepository.findById(1L)).thenReturn(Optional.of(org));
        when(evenementRepository.existsByOrganisateurIdAndTitreIgnoreCaseAndDateDebut(1L, "Concert", debut)).thenReturn(false);
        when(eventMapper.toEntity(dto)).thenReturn(entite);
        when(evenementRepository.save(any(Evenement.class))).thenAnswer(i -> i.getArgument(0));
        when(eventMapper.toResponse(any(Evenement.class))).thenReturn(response);

        EventResponseDTO result = service.create(dto);

        ArgumentCaptor<Evenement> captor = ArgumentCaptor.forClass(Evenement.class);
        verify(evenementRepository).save(captor.capture());
        assertSame(response, result);
        assertEquals(StatutEvenement.BROUILLON, captor.getValue().getStatut());
        assertEquals("Concert", captor.getValue().getTitre());
        assertEquals("Casablanca", captor.getValue().getLieu());
        assertSame(org, captor.getValue().getOrganisateur());
    }

    // ---------- lectures ----------

    @Test
    void findAll_delegueAuMapper() {
        List<Evenement> liste = List.of(new Evenement());
        List<EventResponseDTO> dtos = List.of(mock(EventResponseDTO.class));
        when(evenementRepository.findAll(any(Sort.class))).thenReturn(liste);
        when(eventMapper.toResponseList(liste)).thenReturn(dtos);

        assertSame(dtos, service.findAll());
    }

    @Test
    void findById_trouve() {
        Evenement e = new Evenement();
        EventResponseDTO dto = mock(EventResponseDTO.class);
        when(evenementRepository.findById(1L)).thenReturn(Optional.of(e));
        when(eventMapper.toResponse(e)).thenReturn(dto);

        assertSame(dto, service.findById(1L));
    }

    @Test
    void findById_inconnu_404() {
        when(evenementRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.findById(9L));
    }

    @Test
    void findByOrganisateur_inconnu_404() {
        when(organisateurRepository.existsById(3L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> service.findByOrganisateur(3L));
    }

    @Test
    void findByOrganisateur_ok() {
        List<Evenement> liste = List.of(new Evenement());
        List<EventResponseDTO> dtos = List.of(mock(EventResponseDTO.class));
        when(organisateurRepository.existsById(3L)).thenReturn(true);
        when(evenementRepository.findByOrganisateurIdOrderByDateDebutDesc(3L)).thenReturn(liste);
        when(eventMapper.toResponseList(liste)).thenReturn(dtos);

        assertSame(dtos, service.findByOrganisateur(3L));
    }

    @Test
    void findAVenir_etFindEnVente_deleguent() {
        List<Evenement> liste = List.of(new Evenement());
        List<EventResponseDTO> dtos = List.of(mock(EventResponseDTO.class));
        when(evenementRepository.findByStatutAndDateDebutAfterOrderByDateDebutAsc(eq(StatutEvenement.PUBLIE), any(LocalDateTime.class))).thenReturn(liste);
        when(evenementRepository.findEnVente(eq(StatutEvenement.PUBLIE), any(LocalDateTime.class))).thenReturn(liste);
        when(eventMapper.toResponseList(liste)).thenReturn(dtos);

        assertSame(dtos, service.findAVenir());
        assertSame(dtos, service.findEnVente());
    }

    // ---------- update ----------

    @Test
    void update_evenementAnnule_409() {
        Evenement e = mock(Evenement.class);
        when(evenementRepository.findById(1L)).thenReturn(Optional.of(e));
        when(e.getStatut()).thenReturn(StatutEvenement.ANNULE);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.update(1L, mock(EventRequestDTO.class)));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void update_capaciteInferieureAuxCategories_validationException() {
        Evenement e = mock(Evenement.class);
        EventRequestDTO dto = mock(EventRequestDTO.class);
        when(evenementRepository.findById(1L)).thenReturn(Optional.of(e));
        when(e.getStatut()).thenReturn(StatutEvenement.PUBLIE);
        when(e.getCategories()).thenReturn(List.of(categorie(40), categorie(30)));
        when(dto.capacite()).thenReturn(50);

        assertThrows(ValidationException.class, () -> service.update(1L, dto));
        verify(eventMapper, never()).updateEntity(any(), any());
    }

    @Test
    void update_valide_appliqueLeMapper() {
        Evenement e = mock(Evenement.class);
        EventRequestDTO dto = mock(EventRequestDTO.class);
        EventResponseDTO response = mock(EventResponseDTO.class);
        when(evenementRepository.findById(1L)).thenReturn(Optional.of(e));
        when(e.getStatut()).thenReturn(StatutEvenement.PUBLIE);
        when(e.getCategories()).thenReturn(List.of(categorie(40)));
        when(e.getTitre()).thenReturn(" Titre ");
        when(e.getLieu()).thenReturn(" Lieu ");
        when(dto.capacite()).thenReturn(100);
        when(eventMapper.toResponse(e)).thenReturn(response);

        assertSame(response, service.update(1L, dto));
        verify(eventMapper).updateEntity(dto, e);
        verify(e).setTitre("Titre");
        verify(e).setLieu("Lieu");
    }

    // ---------- publier ----------

    @Test
    void publier_pasBrouillon_409() {
        Evenement e = mock(Evenement.class);
        when(evenementRepository.findById(1L)).thenReturn(Optional.of(e));
        when(e.getStatut()).thenReturn(StatutEvenement.PUBLIE);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.publier(1L));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void publier_sansCategorie_validationException() {
        Evenement e = mock(Evenement.class);
        when(evenementRepository.findById(1L)).thenReturn(Optional.of(e));
        when(e.getStatut()).thenReturn(StatutEvenement.BROUILLON);

        assertThrows(ValidationException.class, () -> service.publier(1L));
    }

    @Test
    void publier_totalDepasseCapacite_validationException() {
        Evenement e = mock(Evenement.class);
        when(evenementRepository.findById(1L)).thenReturn(Optional.of(e));
        when(e.getStatut()).thenReturn(StatutEvenement.BROUILLON);
        when(e.getCategories()).thenReturn(List.of(categorie(80)));
        when(e.getCapacite()).thenReturn(50);

        assertThrows(ValidationException.class, () -> service.publier(1L));
    }

    @Test
    void publier_clotureDepassee_validationException() {
        Evenement e = mock(Evenement.class);
        when(evenementRepository.findById(1L)).thenReturn(Optional.of(e));
        when(e.getStatut()).thenReturn(StatutEvenement.BROUILLON);
        when(e.getCategories()).thenReturn(List.of(categorie(10)));
        when(e.getCapacite()).thenReturn(100);
        when(e.getDateClotureVente()).thenReturn(LocalDateTime.now().minusDays(1));

        assertThrows(ValidationException.class, () -> service.publier(1L));
        verify(e, never()).setStatut(StatutEvenement.PUBLIE);
    }

    @Test
    void publier_valide_passeEnPublie() {
        Evenement e = mock(Evenement.class);
        EventResponseDTO response = mock(EventResponseDTO.class);
        when(evenementRepository.findById(1L)).thenReturn(Optional.of(e));
        when(e.getStatut()).thenReturn(StatutEvenement.BROUILLON);
        when(e.getCategories()).thenReturn(List.of(categorie(10)));
        when(e.getCapacite()).thenReturn(100);
        when(e.getDateClotureVente()).thenReturn(LocalDateTime.now().plusDays(5));
        when(eventMapper.toResponse(e)).thenReturn(response);

        assertSame(response, service.publier(1L));
        verify(e).setStatut(StatutEvenement.PUBLIE);
    }

    // ---------- annuler ----------

    @Test
    void annuler_dejaAnnuleOuTermine_409() {
        Evenement e = mock(Evenement.class);
        when(evenementRepository.findById(1L)).thenReturn(Optional.of(e));
        when(e.getStatut()).thenReturn(StatutEvenement.TERMINE);

        assertThrows(ResponseStatusException.class, () -> service.annuler(1L));
        verifyNoInteractions(remboursementService);
    }

    @Test
    void annuler_valide_declencheLesRemboursements() {
        Evenement e = mock(Evenement.class);
        when(evenementRepository.findById(1L)).thenReturn(Optional.of(e));
        when(e.getStatut()).thenReturn(StatutEvenement.PUBLIE);

        service.annuler(1L);

        verify(e).setStatut(StatutEvenement.ANNULE);
        verify(remboursementService).rembourserEvenementAnnule(e);
    }

    // ---------- delete ----------

    @Test
    void delete_pasBrouillon_409() {
        Evenement e = mock(Evenement.class);
        when(evenementRepository.findById(1L)).thenReturn(Optional.of(e));
        when(e.getStatut()).thenReturn(StatutEvenement.PUBLIE);

        assertThrows(ResponseStatusException.class, () -> service.delete(1L));
        verify(evenementRepository, never()).delete(any());
    }

    @Test
    void delete_brouillon_supprime() {
        Evenement e = mock(Evenement.class);
        when(evenementRepository.findById(1L)).thenReturn(Optional.of(e));
        when(e.getStatut()).thenReturn(StatutEvenement.BROUILLON);

        service.delete(1L);

        verify(promotionRepository).deleteAll(any());
        verify(evenementRepository).delete(e);
    }

    private CategorieBillet categorie(int quantite) {
        CategorieBillet c = new CategorieBillet();
        c.setQuantiteTotal(quantite);
        return c;
    }
}
