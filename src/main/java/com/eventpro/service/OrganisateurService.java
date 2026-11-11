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
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class OrganisateurService {

    private final OrganisateurRepository organisateurRepository;
    private final EvenementRepository evenementRepository;
    private final PromotionRepository promotionRepository;
    private final UtilisateurService utilisateurService;
    private final UtilisateurMapper utilisateurMapper;

    public OrganisateurService(OrganisateurRepository organisateurRepository,
                               EvenementRepository evenementRepository,
                               PromotionRepository promotionRepository,
                               UtilisateurService utilisateurService,
                               UtilisateurMapper utilisateurMapper) {
        this.organisateurRepository = organisateurRepository;
        this.evenementRepository = evenementRepository;
        this.promotionRepository = promotionRepository;
        this.utilisateurService = utilisateurService;
        this.utilisateurMapper = utilisateurMapper;
    }

    @Transactional
    public UtilisateurResponseDTO create(OrganisateurRequestDTO dto) {

        utilisateurService.verifierEmailLibre(dto.email());

        Organisateur organisateur = utilisateurMapper.toOrganisateur(dto);     // nom, email
        organisateur.setMotDePasse(utilisateurService.hasher(dto.motDePasse()));
        utilisateurService.nettoyer(organisateur);

        return utilisateurMapper.toResponse(organisateurRepository.save(organisateur));
    }

    @Transactional(readOnly = true)
    public List<UtilisateurResponseDTO> findAll() {
        return organisateurRepository.findAll(Sort.by("id")).stream()
                .map(utilisateurMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UtilisateurResponseDTO findById(Long id) {
        return utilisateurMapper.toResponse(findOrganisateur(id));
    }

    @Transactional
    public UtilisateurResponseDTO update(Long id, OrganisateurUpdateDTO dto) {

        Organisateur organisateur = findOrganisateur(id);
        utilisateurService.verifierEmailLibre(dto.email(), id);

        utilisateurMapper.updateOrganisateur(dto, organisateur);
        utilisateurService.nettoyer(organisateur);

        return utilisateurMapper.toResponse(organisateur);
    }

    @Transactional
    public void delete(Long id) {

        Organisateur organisateur = findOrganisateur(id);

        if (evenementRepository.existsByOrganisateurId(id)
                || promotionRepository.existsByOrganisateurId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cet organisateur a des evenements ou des promotions : desactivez son compte au lieu de le supprimer");
        }

        organisateurRepository.delete(organisateur);
    }

    // ---------- helper ----------
    private Organisateur findOrganisateur(Long id) {
        return organisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organisateur introuvable : " + id));
    }
}
