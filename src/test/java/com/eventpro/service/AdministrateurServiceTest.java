package com.eventpro.service;

import com.eventpro.dto.user.AdministrateurRequestDTO;
import com.eventpro.dto.user.UtilisateurResponseDTO;
import com.eventpro.entity.Administrateur;
import com.eventpro.exception.ValidationException;
import com.eventpro.mapper.UtilisateurMapper;
import com.eventpro.repository.AdministrateurRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdministrateurServiceTest {

    @Mock AdministrateurRepository administrateurRepository;
    @Mock UtilisateurService utilisateurService;
    @Mock UtilisateurMapper utilisateurMapper;

    @InjectMocks AdministrateurService service;

    @Test
    void create_valide() {
        AdministrateurRequestDTO dto = mock(AdministrateurRequestDTO.class);
        Administrateur admin = new Administrateur();
        UtilisateurResponseDTO response = mock(UtilisateurResponseDTO.class);
        when(dto.email()).thenReturn("admin@b.com");
        when(dto.motDePasse()).thenReturn("secret");
        when(utilisateurMapper.toAdministrateur(dto)).thenReturn(admin);
        when(utilisateurService.hasher("secret")).thenReturn("HASH");
        when(administrateurRepository.save(admin)).thenReturn(admin);
        when(utilisateurMapper.toResponse(admin)).thenReturn(response);

        assertSame(response, service.create(dto));
        assertEquals("HASH", admin.getMotDePasse());
        verify(utilisateurService).nettoyer(admin);
    }

    @Test
    void create_emailDejaUtilise_pasDeSauvegarde() {
        AdministrateurRequestDTO dto = mock(AdministrateurRequestDTO.class);
        when(dto.email()).thenReturn("admin@b.com");
        doThrow(new ValidationException("Cet email est deja utilise")).when(utilisateurService).verifierEmailLibre("admin@b.com");

        assertThrows(ValidationException.class, () -> service.create(dto));
        verify(administrateurRepository, never()).save(any());
    }
}
