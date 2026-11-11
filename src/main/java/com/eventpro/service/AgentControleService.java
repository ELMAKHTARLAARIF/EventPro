package com.eventpro.service;

import com.eventpro.dto.user.AgentControleRequestDTO;
import com.eventpro.dto.user.UtilisateurResponseDTO;
import com.eventpro.entity.AgentControle;
import com.eventpro.mapper.UtilisateurMapper;
import com.eventpro.repository.AgentControleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentControleService {

    private final AgentControleRepository agentControleRepository;
    private final UtilisateurService utilisateurService;
    private final UtilisateurMapper utilisateurMapper;

    public AgentControleService(AgentControleRepository agentControleRepository, UtilisateurService utilisateurService, UtilisateurMapper utilisateurMapper) {
        this.agentControleRepository = agentControleRepository;
        this.utilisateurService = utilisateurService;
        this.utilisateurMapper = utilisateurMapper;
    }

    @Transactional
    public UtilisateurResponseDTO create(AgentControleRequestDTO dto) {

        utilisateurService.verifierEmailLibre(dto.email());

        AgentControle agent = utilisateurMapper.toAgentControle(dto);
        agent.setMotDePasse(utilisateurService.hasher(dto.motDePasse()));
        utilisateurService.nettoyer(agent);

        return utilisateurMapper.toResponse(agentControleRepository.save(agent));
    }
}
