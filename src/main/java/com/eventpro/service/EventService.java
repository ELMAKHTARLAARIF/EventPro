package com.eventpro.service;

import com.eventpro.dto.EventRequestDTO;
import com.eventpro.dto.EventResponseDTO;
import com.eventpro.entity.CategorieBillet;
import com.eventpro.entity.Evenement;
import com.eventpro.entity.Organisateur;
import com.eventpro.enums.StatutEvenement;
import com.eventpro.mapper.EventMapper;
import com.eventpro.repository.EvenementRepository;
import com.eventpro.repository.OrganisateurRepository;
import com.eventpro.repository.PromotionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public EventService(EvenementRepository evenementRepository, OrganisateurRepository organisateurRepository, PromotionRepository promotionRepository, EventMapper eventMapper) {
        this.evenementRepository = evenementRepository;
        this.organisateurRepository = organisateurRepository;
        this.promotionRepository = promotionRepository;
        this.eventMapper = eventMapper;
    }


    @Transactional
    public EventResponseDTO create(EventRequestDTO dto) {

        if (dto.organisateurId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'organisateur est obligatoire");
        }

        Organisateur organisateur = organisateurRepository.findById(dto.organisateurId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organisateur introuvable : " + dto.organisateurId()));

        if (dto.dateDebut().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de debut doit etre dans le futur");
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
    public Page<EventResponseDTO> search(StatutEvenement statut, String q, Pageable pageable) {
        String keyword = (q == null) ? "" : q.trim();
        return evenementRepository.search(statut, keyword, pageable).map(eventMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public EventResponseDTO findById(Long id) {
        return eventMapper.toResponse(findEvenement(id));
    }

    @Transactional(readOnly = true)
    public List<EventResponseDTO> findByOrganisateur(Long organisateurId) {
        if (!organisateurRepository.existsById(organisateurId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Organisateur introuvable : " + organisateurId);
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La capacite ne peut pas etre inferieure au total des categories de billets (" + totalCategories + ")");
        }

        eventMapper.updateEntity(dto, evenement);   // fills the existing managed entity
        nettoyer(evenement);

        // no save() needed: the entity is managed, Hibernate flushes the changes
        return eventMapper.toResponse(evenement);
    }

    @Transactional
    public EventResponseDTO publier(Long id) {

        Evenement evenement = findEvenement(id);

        if (evenement.getStatut() != StatutEvenement.BROUILLON) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seul un evenement en brouillon peut etre publie");
        }

        if (evenement.getCategories().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ajoutez au moins une categorie de billet avant de publier");
        }

        int total = totalBillets(evenement);
        if (total > evenement.getCapacite()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le total des billets (" + total + ") depasse la capacite (" + evenement.getCapacite() + ")");
        }

        if (!evenement.getDateClotureVente().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de cloture des ventes est deja depassee");
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
        // TODO: cancel the pending reservations and create the refunds for paid ones
        return eventMapper.toResponse(evenement);
    }

    @Transactional
    public void delete(Long id) {

        Evenement evenement = findEvenement(id);

        // a draft has no reservation and no ticket yet, so it is safe to remove
        if (evenement.getStatut() != StatutEvenement.BROUILLON) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seul un evenement en brouillon peut etre supprime. " + "Pour un evenement publie, utilisez l'annulation.");
        }

        promotionRepository.deleteAll(evenement.getPromotions());
        evenementRepository.delete(evenement); // categories are removed by cascade
    }

    // ---------- helpers ----------
    private Evenement findEvenement(Long id) {
        return evenementRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evenement introuvable : " + id));
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