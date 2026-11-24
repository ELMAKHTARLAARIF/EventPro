package com.eventpro.service;

import com.eventpro.dto.user.OrganisateurRequestDTO;
import com.eventpro.dto.user.OrganisateurUpdateDTO;
import com.eventpro.dto.user.UtilisateurResponseDTO;
import com.eventpro.entity.Organisateur;
import com.eventpro.exception.ResourceNotFoundException;
import com.eventpro.mapper.UtilisateurMapper;
import com.eventpro.repository.EvenementRepository;
import com.eventpro.repository.OrganisateurRepository;
import com.eventpro.repository.PromotionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganisateurServiceTest {

    @Mock OrganisateurRepository organisateurRepository;
    @Mock EvenementRepository evenementRepository;
    @Mock PromotionRepository promotionRepository;
    @Mock UtilisateurService utilisateurService;
    @Mock UtilisateurMapper utilisateurMapper;

    @InjectMocks OrganisateurService service;

    @Test
    void create_valide() {
        OrganisateurRequestDTO dto = mock(OrganisateurRequestDTO.class);
        Organisateur o = new Organisateur();
        UtilisateurResponseDTO response = mock(UtilisateurResponseDTO.class);
        when(dto.email()).thenReturn("o@b.com");
        when(dto.motDePasse()).thenReturn("secret");
        when(utilisateurMapper.toOrganisateur(dto)).thenReturn(o);
        when(utilisateurService.hasher("secret")).thenReturn("HASH");
        when(organisateurRepository.save(o)).thenReturn(o);
        when(utilisateurMapper.toResponse(o)).thenReturn(response);

        assertSame(response, service.create(dto));
        assertEquals("HASH", o.getMotDePasse());
        verify(utilisateurService).verifierEmailLibre("o@b.com");
        verify(utilisateurService).nettoyer(o);
    }

    @Test
    void findAll_mappeLesOrganisateurs() {
        Organisateur o = new Organisateur();
        UtilisateurResponseDTO dto = mock(UtilisateurResponseDTO.class);
        when(organisateurRepository.findAll(any(Sort.class))).thenReturn(List.of(o));
        when(utilisateurMapper.toResponse(o)).thenReturn(dto);

        assertEquals(List.of(dto), service.findAll());
    }

    @Test
    void findById_trouve() {
        Organisateur o = new Organisateur();
        UtilisateurResponseDTO dto = mock(UtilisateurResponseDTO.class);
        when(organisateurRepository.findById(1L)).thenReturn(Optional.of(o));
        when(utilisateurMapper.toResponse(o)).thenReturn(dto);

        assertSame(dto, service.findById(1L));
    }

    @Test
    void findById_inconnu_404() {
        when(organisateurRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.findById(1L));
    }

    @Test
    void update_valide() {
        OrganisateurUpdateDTO dto = mock(OrganisateurUpdateDTO.class);
        Organisateur o = new Organisateur();
        UtilisateurResponseDTO response = mock(UtilisateurResponseDTO.class);
        when(dto.email()).thenReturn("o@b.com");
        when(organisateurRepository.findById(1L)).thenReturn(Optional.of(o));
        when(utilisateurMapper.toResponse(o)).thenReturn(response);

        assertSame(response, service.update(1L, dto));
        verify(utilisateurService).verifierEmailLibre("o@b.com", 1L);
        verify(utilisateurMapper).updateOrganisateur(dto, o);
        verify(utilisateurService).nettoyer(o);
    }

    @Test
    void delete_avecEvenements_409() {
        Organisateur o = new Organisateur();
        when(organisateurRepository.findById(1L)).thenReturn(Optional.of(o));
        when(evenementRepository.existsByOrganisateurId(1L)).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> service.delete(1L));
        verify(organisateurRepository, never()).delete(any());
    }

    @Test
    void delete_avecPromotions_409() {
        Organisateur o = new Organisateur();
        when(organisateurRepository.findById(1L)).thenReturn(Optional.of(o));
        when(evenementRepository.existsByOrganisateurId(1L)).thenReturn(false);
        when(promotionRepository.existsByOrganisateurId(1L)).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> service.delete(1L));
    }

    @Test
    void delete_sansDependances_supprime() {
        Organisateur o = new Organisateur();
        when(organisateurRepository.findById(1L)).thenReturn(Optional.of(o));
        when(evenementRepository.existsByOrganisateurId(1L)).thenReturn(false);
        when(promotionRepository.existsByOrganisateurId(1L)).thenReturn(false);

        service.delete(1L);

        verify(organisateurRepository).delete(o);
    }
}
