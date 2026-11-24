package com.eventpro.service;

import com.eventpro.dto.user.ClientRequestDTO;
import com.eventpro.dto.user.ClientUpdateDTO;
import com.eventpro.dto.user.UtilisateurResponseDTO;
import com.eventpro.entity.Client;
import com.eventpro.exception.ResourceNotFoundException;
import com.eventpro.exception.ValidationException;
import com.eventpro.mapper.UtilisateurMapper;
import com.eventpro.repository.ClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock ClientRepository clientRepository;
    @Mock UtilisateurService utilisateurService;
    @Mock UtilisateurMapper utilisateurMapper;

    @InjectMocks ClientService service;

    @Test
    void create_valide_hasheLeMotDePasseEtSauvegarde() {
        ClientRequestDTO dto = mock(ClientRequestDTO.class);
        Client client = new Client();
        UtilisateurResponseDTO response = mock(UtilisateurResponseDTO.class);
        when(dto.email()).thenReturn("a@b.com");
        when(dto.motDePasse()).thenReturn("secret");
        when(utilisateurMapper.toClient(dto)).thenReturn(client);
        when(utilisateurService.hasher("secret")).thenReturn("HASH");
        when(clientRepository.save(client)).thenReturn(client);
        when(utilisateurMapper.toResponse(client)).thenReturn(response);

        assertSame(response, service.create(dto));
        assertEquals("HASH", client.getMotDePasse());
        verify(utilisateurService).verifierEmailLibre("a@b.com");
        verify(utilisateurService).nettoyer(client);
    }

    @Test
    void create_emailDejaUtilise_pasDeSauvegarde() {
        ClientRequestDTO dto = mock(ClientRequestDTO.class);
        when(dto.email()).thenReturn("a@b.com");
        doThrow(new ValidationException("Cet email est deja utilise")).when(utilisateurService).verifierEmailLibre("a@b.com");

        assertThrows(ValidationException.class, () -> service.create(dto));
        verify(clientRepository, never()).save(any());
    }

    @Test
    void findAll_mappeLesClients() {
        Client c = new Client();
        UtilisateurResponseDTO dto = mock(UtilisateurResponseDTO.class);
        when(clientRepository.findAll(any(Sort.class))).thenReturn(List.of(c));
        when(utilisateurMapper.toResponse(c)).thenReturn(dto);

        assertEquals(List.of(dto), service.findAll());
    }

    @Test
    void findById_trouve() {
        Client c = new Client();
        UtilisateurResponseDTO dto = mock(UtilisateurResponseDTO.class);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(c));
        when(utilisateurMapper.toResponse(c)).thenReturn(dto);

        assertSame(dto, service.findById(1L));
    }

    @Test
    void findById_inconnu_404() {
        when(clientRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.findById(1L));
    }

    @Test
    void update_valide_metAJourLeClient() {
        ClientUpdateDTO dto = mock(ClientUpdateDTO.class);
        Client c = new Client();
        UtilisateurResponseDTO response = mock(UtilisateurResponseDTO.class);
        when(dto.email()).thenReturn("a@b.com");
        when(clientRepository.findById(1L)).thenReturn(Optional.of(c));
        when(utilisateurMapper.toResponse(c)).thenReturn(response);

        assertSame(response, service.update(1L, dto));
        verify(utilisateurService).verifierEmailLibre("a@b.com", 1L);
        verify(utilisateurMapper).updateClient(dto, c);
        verify(utilisateurService).nettoyer(c);
    }

    @Test
    void update_clientInconnu_404() {
        when(clientRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.update(1L, mock(ClientUpdateDTO.class)));
    }
}
