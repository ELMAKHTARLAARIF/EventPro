package com.eventpro.service;

import com.eventpro.dto.EventRequestDTO;
import com.eventpro.dto.EventResponseDTO;
import com.eventpro.entity.CategorieBillet;
import com.eventpro.entity.Evenement;
import com.eventpro.entity.Organisateur;
import com.eventpro.enums.StatutEvenement;
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

    public EventService(EvenementRepository evenementRepository, OrganisateurRepository organisateurRepository, PromotionRepository promotionRepository) {
        this.evenementRepository = evenementRepository;
        this.organisateurRepository = organisateurRepository;
        this.promotionRepository = promotionRepository;
    }

    // ---------- CREATE ----------
    @Transactional
    public EventResponseDTO create(EventRequestDTO dto) {

        if (dto.organisateurId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'organisateur est obligatoire");
        }

        Organisateur organisateur = organisateurRepository.findById(dto.organisateurId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organisateur introuvable : " + dto.organisateurId()));

        if (!dto.dateDebut().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de debut doit etre dans le futur");
        }

        if (evenementRepository.existsByOrganisateurIdAndTitreIgnoreCaseAndDateDebut(organisateur.getId(), dto.titre().trim(), dto.dateDebut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vous avez deja un evenement avec ce titre a cette date");
        }

        Evenement evenement = new Evenement();
        apply(evenement, dto);
        evenement.setOrganisateur(organisateur);
        evenement.setStatut(StatutEvenement.BROUILLON);

        return toResponse(evenementRepository.save(evenement));
    }

    // ---------- READ ----------
    @Transactional(readOnly = true)
    public Page<EventResponseDTO> search(StatutEvenement statut, String q, Pageable pageable) {
        String keyword = (q == null) ? "" : q.trim();
        return evenementRepository.search(statut, keyword, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EventResponseDTO findById(Long id) {
        return toResponse(findEvenement(id));
    }

    @Transactional(readOnly = true)
    public List<EventResponseDTO> findByOrganisateur(Long organisateurId) {
        if (!organisateurRepository.existsById(organisateurId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Organisateur introuvable : " + organisateurId);
        }
        return evenementRepository.findByOrganisateurIdOrderByDateDebutDesc(organisateurId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EventResponseDTO> findAVenir() {
        return evenementRepository.findByStatutAndDateDebutAfterOrderByDateDebutAsc(StatutEvenement.PUBLIE, LocalDateTime.now()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EventResponseDTO> findEnVente() {
        return evenementRepository.findEnVente(StatutEvenement.PUBLIE, LocalDateTime.now()).stream().map(this::toResponse).toList();
    }

    // ---------- UPDATE ----------
    @Transactional
    public EventResponseDTO update(Long id, EventRequestDTO dto) {

        Evenement evenement = findEvenement(id);
        verifierModifiable(evenement);

        int totalCategories = totalBillets(evenement);
        if (dto.capacite() < totalCategories) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La capacite ne peut pas etre inferieure au total des categories de billets (" + totalCategories + ")");
        }

        apply(evenement, dto);

        // no save() needed: the entity is managed, Hibernate flushes the changes
        return toResponse(evenement);
    }

    // ---------- STATUS CHANGES ----------
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
        return toResponse(evenement);
    }

    @Transactional
    public EventResponseDTO annuler(Long id) {

        Evenement evenement = findEvenement(id);

        if (evenement.getStatut() == StatutEvenement.ANNULE || evenement.getStatut() == StatutEvenement.TERMINE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet evenement est deja annule ou termine");
        }

        evenement.setStatut(StatutEvenement.ANNULE);
        // TODO: cancel the pending reservations and create the refunds for paid ones
        return toResponse(evenement);
    }

    // ---------- DELETE ----------
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

    private void apply(Evenement e, EventRequestDTO dto) {
        e.setTitre(dto.titre().trim());
        e.setDescription(dto.description());
        e.setLieu(dto.lieu().trim());
        e.setDateDebut(dto.dateDebut());
        e.setDateFin(dto.dateFin());
        e.setDateOuvertureVente(dto.dateOuvertureVente());
        e.setDateClotureVente(dto.dateClotureVente());
        e.setCapacite(dto.capacite());
    }

    private EventResponseDTO toResponse(Evenement e) {
        return new EventResponseDTO(e.getId(), e.getTitre(), e.getDescription(), e.getLieu(), e.getDateDebut(), e.getDateFin(), e.getDateOuvertureVente(), e.getDateClotureVente(), e.getStatut(), e.getCapacite(), e.getOrganisateur().getId(), e.getOrganisateur().getNomOrganisation());
    }
}