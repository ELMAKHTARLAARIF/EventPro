package com.eventpro.service;

import com.eventpro.dto.event.EventRequestDTO;
import com.eventpro.dto.event.EventResponseDTO;
import com.eventpro.entity.CategorieBillet;
import com.eventpro.entity.Evenement;
import com.eventpro.entity.Organisateur;
import com.eventpro.enums.StatutEvenement;
// EXCEPTION: our custom business exceptions (handled by GlobalExceptionHandler)
import com.eventpro.exception.ResourceNotFoundException;
import com.eventpro.exception.ValidationException;
import com.eventpro.mapper.EventMapper;
import com.eventpro.repository.EvenementRepository;
import com.eventpro.repository.OrganisateurRepository;
import com.eventpro.repository.PromotionRepository;
import com.eventpro.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventService {

    private final EvenementRepository evenementRepository;
    private final OrganisateurRepository organisateurRepository;
    private final PromotionRepository promotionRepository;
    private final EventMapper eventMapper;
    private final RemboursementService remboursementService;


    public EventService(EvenementRepository evenementRepository, OrganisateurRepository organisateurRepository, PromotionRepository promotionRepository, EventMapper eventMapper, RemboursementService remboursementService, UserRepository userRepository) {
        this.evenementRepository = evenementRepository;
        this.organisateurRepository = organisateurRepository;
        this.promotionRepository = promotionRepository;
        this.eventMapper = eventMapper;
        this.remboursementService = remboursementService;
    }

    @Transactional
    public EventResponseDTO create(EventRequestDTO dto) {

        if (dto.organisateurId() == null) {
            throw new ValidationException("L'organisateur est obligatoire");
        }

        Organisateur organisateur = organisateurRepository.findById(dto.organisateurId()).orElseThrow(() -> new ResourceNotFoundException("Organisateur introuvable : " + dto.organisateurId()));

        if (dto.dateDebut().isBefore(LocalDateTime.now())) {
            throw new ValidationException("La date de debut doit etre dans le futur");
        }

        if (evenementRepository.existsByOrganisateurIdAndTitreIgnoreCaseAndDateDebut(organisateur.getId(), dto.titre().trim(), dto.dateDebut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vous avez deja un evenement avec ce titre a cette date");
        }

        Evenement evenement = eventMapper.toEntity(dto);   // DTO -> entity
        nettoyer(evenement);
        evenement.setOrganisateur(organisateur);           // needs a DB lookup: done here
        evenement.setStatut(StatutEvenement.BROUILLON);    // business rule: done here

        return eventMapper.toResponse(evenementRepository.save(evenement));
    }

    @Transactional(readOnly = true)
    public List<EventResponseDTO> findAll() {
        return eventMapper.toResponseList(evenementRepository.findAll(Sort.by("dateDebut")));
    }

    @Transactional(readOnly = true)
    public EventResponseDTO findById(Long id) {
        return eventMapper.toResponse(findEvenement(id));
    }

    @Transactional(readOnly = true)
    public List<EventResponseDTO> findByOrganisateur(Long organisateurId) {
        if (!organisateurRepository.existsById(organisateurId)) {
            throw new ResourceNotFoundException("Organisateur introuvable : " + organisateurId);
        }
        return eventMapper.toResponseList(evenementRepository.findByOrganisateurIdOrderByDateDebutDesc(organisateurId));
    }

    @Transactional(readOnly = true)
    public List<EventResponseDTO> findAVenir() {
        return eventMapper.toResponseList(evenementRepository.findByStatutAndDateDebutAfterOrderByDateDebutAsc(StatutEvenement.PUBLIE, LocalDateTime.now()));
    }

    @Transactional(readOnly = true)
    public List<EventResponseDTO> findEnVente() {
        return eventMapper.toResponseList(evenementRepository.findEnVente(StatutEvenement.PUBLIE, LocalDateTime.now()));
    }

    @Transactional
    public EventResponseDTO update(Long id, EventRequestDTO dto) {

        Evenement evenement = findEvenement(id);
        verifierModifiable(evenement);

        int totalCategories = totalBillets(evenement);
        if (dto.capacite() < totalCategories) {
            throw new ValidationException("La capacite ne peut pas etre inferieure au total des categories de billets (" + totalCategories + ")");
        }

        eventMapper.updateEntity(dto, evenement);
        nettoyer(evenement);

        return eventMapper.toResponse(evenement);
    }

    @Transactional
    public EventResponseDTO publier(Long id) {

        Evenement evenement = findEvenement(id);

        if (evenement.getStatut() != StatutEvenement.BROUILLON) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seul un evenement en brouillon peut etre publie");
        }

        if (evenement.getCategories().isEmpty()) {
            throw new ValidationException("Ajoutez au moins une categorie de billet avant de publier");
        }

        int total = totalBillets(evenement);
        if (total > evenement.getCapacite()) {
            throw new ValidationException("Le total des billets (" + total + ") depasse la capacite (" + evenement.getCapacite() + ")");
        }

        if (!evenement.getDateClotureVente().isAfter(LocalDateTime.now())) {
            throw new ValidationException("La date de cloture des ventes est deja depassee");
        }

        evenement.setStatut(StatutEvenement.PUBLIE);
        return eventMapper.toResponse(evenement);
    }

    @Transactional
    public EventResponseDTO annuler(Long id) {

        Evenement evenement = findEvenement(id);

        if (evenement.getStatut() == StatutEvenement.ANNULE || evenement.getStatut() == StatutEvenement.TERMINE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet evenement est deja annule ou termine");
        }

        evenement.setStatut(StatutEvenement.ANNULE);
        remboursementService.rembourserEvenementAnnule(evenement);   // 100% for every buyer
        return eventMapper.toResponse(evenement);
    }

    @Transactional
    public void delete(Long id) {

        Evenement evenement = findEvenement(id);

        if (evenement.getStatut() != StatutEvenement.BROUILLON) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seul un evenement en brouillon peut etre supprime. " + "Pour un evenement publie, utilisez l'annulation.");
        }

        promotionRepository.deleteAll(evenement.getPromotions());
        evenementRepository.delete(evenement); // categories are removed by cascade
    }

    // ---------- helpers ----------
    private Evenement findEvenement(Long id) {
        return evenementRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Evenement introuvable : " + id));
    }

    private void verifierModifiable(Evenement evenement) {
        if (evenement.getStatut() == StatutEvenement.ANNULE || evenement.getStatut() == StatutEvenement.TERMINE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un evenement annule ou termine ne peut plus etre modifie");
        }
    }

    private int totalBillets(Evenement evenement) {
        return evenement.getCategories().stream().mapToInt(CategorieBillet::getQuantiteTotal).sum();
    }

    // the mapper copies values as they are, so we trim the text fields here
    private void nettoyer(Evenement e) {
        e.setTitre(e.getTitre().trim());
        e.setLieu(e.getLieu().trim());
    }

}