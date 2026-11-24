package com.eventpro.service;

import com.eventpro.dto.user.UtilisateurResponseDTO;
import com.eventpro.entity.Client;
import com.eventpro.entity.User;
import com.eventpro.exception.ResourceNotFoundException;
import com.eventpro.exception.ValidationException;
import com.eventpro.mapper.UtilisateurMapper;
import com.eventpro.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UtilisateurServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock UtilisateurMapper utilisateurMapper;

    @InjectMocks UtilisateurService service;

    @Test
    void verifierEmailLibre_emailPris_validationException() {
        when(userRepository.existsByEmailIgnoreCase("a@b.com")).thenReturn(true);
        assertThrows(ValidationException.class, () -> service.verifierEmailLibre(" a@b.com "));
    }

    @Test
    void verifierEmailLibre_emailLibre_ok() {
        when(userRepository.existsByEmailIgnoreCase("a@b.com")).thenReturn(false);
        assertDoesNotThrow(() -> service.verifierEmailLibre("a@b.com"));
    }

    @Test
    void verifierEmailLibre_autreCompte_emailPris_validationException() {
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("a@b.com", 5L)).thenReturn(true);
        assertThrows(ValidationException.class, () -> service.verifierEmailLibre("a@b.com", 5L));
    }

    @Test
    void verifierEmailLibre_autreCompte_ok() {
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("a@b.com", 5L)).thenReturn(false);
        assertDoesNotThrow(() -> service.verifierEmailLibre("a@b.com", 5L));
    }

    @Test
    void hasher_utiliseLEncodeur() {
        when(passwordEncoder.encode("secret")).thenReturn("HASH");
        assertEquals("HASH", service.hasher("secret"));
    }

    @Test
    void nettoyer_trimNomEtEmailEnMinuscules() {
        Client c = new Client();
        c.setNom("  Ali  ");
        c.setEmail("  ALI@Mail.COM ");

        service.nettoyer(c);

        assertEquals("Ali", c.getNom());
        assertEquals("ali@mail.com", c.getEmail());
    }

    @Test
    void findAll_mappeTousLesUtilisateurs() {
        User u = new Client();
        UtilisateurResponseDTO dto = mock(UtilisateurResponseDTO.class);
        when(userRepository.findAll(any(Sort.class))).thenReturn(List.of(u));
        when(utilisateurMapper.toResponse(u)).thenReturn(dto);

        assertEquals(List.of(dto), service.findAll());
    }

    @Test
    void findById_trouve() {
        User u = new Client();
        UtilisateurResponseDTO dto = mock(UtilisateurResponseDTO.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(utilisateurMapper.toResponse(u)).thenReturn(dto);

        assertSame(dto, service.findById(1L));
    }

    @Test
    void findById_inconnu_404() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.findById(1L));
    }

    @Test
    void changerActivation_desactiveLeCompte() {
        User u = new Client();
        u.setActif(true);
        UtilisateurResponseDTO dto = mock(UtilisateurResponseDTO.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(utilisateurMapper.toResponse(u)).thenReturn(dto);

        assertSame(dto, service.changerActivation(1L, false));
        assertFalse(u.isActif());
    }

    @Test
    void changerActivation_inconnu_404() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.changerActivation(1L, true));
    }
}
