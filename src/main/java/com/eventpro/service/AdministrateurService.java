package com.eventpro.service;

import com.eventpro.dto.user.AdministrateurRequestDTO;
import com.eventpro.dto.user.UtilisateurResponseDTO;
import com.eventpro.entity.Administrateur;
import com.eventpro.mapper.UtilisateurMapper;
import com.eventpro.repository.AdministrateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdministrateurService {

    private final AdministrateurRepository administrateurRepository;
    private final UtilisateurService utilisateurService;
    private final UtilisateurMapper utilisateurMapper;

    public AdministrateurService(AdministrateurRepository administrateurRepository, UtilisateurService utilisateurService, UtilisateurMapper utilisateurMapper) {
        this.administrateurRepository = administrateurRepository;
        this.utilisateurService = utilisateurService;
        this.utilisateurMapper = utilisateurMapper;
    }

    @Transactional
    public UtilisateurResponseDTO create(AdministrateurRequestDTO dto) {

        utilisateurService.verifierEmailLibre(dto.email());

        Administrateur administrateur = utilisateurMapper.toAdministrateur(dto);
        administrateur.setMotDePasse(utilisateurService.hasher(dto.motDePasse()));
        utilisateurService.nettoyer(administrateur);

        return utilisateurMapper.toResponse(administrateurRepository.save(administrateur));
    }
}
