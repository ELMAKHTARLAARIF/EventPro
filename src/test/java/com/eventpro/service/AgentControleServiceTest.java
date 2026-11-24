package com.eventpro.service;

import com.eventpro.dto.user.AgentControleRequestDTO;
import com.eventpro.dto.user.UtilisateurResponseDTO;
import com.eventpro.entity.AgentControle;
import com.eventpro.exception.ValidationException;
import com.eventpro.mapper.UtilisateurMapper;
import com.eventpro.repository.AgentControleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentControleServiceTest {

    @Mock AgentControleRepository agentControleRepository;
    @Mock UtilisateurService utilisateurService;
    @Mock UtilisateurMapper utilisateurMapper;

    @InjectMocks AgentControleService service;

    @Test
    void create_valide() {
        AgentControleRequestDTO dto = mock(AgentControleRequestDTO.class);
        AgentControle agent = new AgentControle();
        UtilisateurResponseDTO response = mock(UtilisateurResponseDTO.class);
        when(dto.email()).thenReturn("agent@b.com");
        when(dto.motDePasse()).thenReturn("secret");
        when(utilisateurMapper.toAgentControle(dto)).thenReturn(agent);
        when(utilisateurService.hasher("secret")).thenReturn("HASH");
        when(agentControleRepository.save(agent)).thenReturn(agent);
        when(utilisateurMapper.toResponse(agent)).thenReturn(response);

        assertSame(response, service.create(dto));
        assertEquals("HASH", agent.getMotDePasse());
        verify(utilisateurService).nettoyer(agent);
    }

    @Test
    void create_emailDejaUtilise_pasDeSauvegarde() {
        AgentControleRequestDTO dto = mock(AgentControleRequestDTO.class);
        when(dto.email()).thenReturn("agent@b.com");
        doThrow(new ValidationException("Cet email est deja utilise")).when(utilisateurService).verifierEmailLibre("agent@b.com");

        assertThrows(ValidationException.class, () -> service.create(dto));
        verify(agentControleRepository, never()).save(any());
    }
}
