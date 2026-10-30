package com.eventpro.service;

import com.eventpro.dto.CategorieBilletRequestDTO;
import com.eventpro.dto.CategorieBilletResponseDTO;
import com.eventpro.entity.CategorieBillet;
import com.eventpro.entity.Evenement;
import com.eventpro.enums.StatutEvenement;
import com.eventpro.mapper.CategorieBilletMapper;
import com.eventpro.repository.CategorieBilletRepository;
import com.eventpro.repository.EvenementRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CategorieBilletService {

    private final CategorieBilletRepository categorieRepository;
    private final EvenementRepository evenementRepository;
    private final CategorieBilletMapper categorieMapper;

    public CategorieBilletService(CategorieBilletRepository categorieRepository, EvenementRepository evenementRepository, CategorieBilletMapper categorieMapper) {
        this.categorieRepository = categorieRepository;
        this.evenementRepository = evenementRepository;
        this.categorieMapper = categorieMapper;
    }

    // ---------- CREATE ----------
    @Transactional
    public CategorieBilletResponseDTO create(Long evenementId, CategorieBilletRequestDTO dto) {

        Evenement evenement = findEvenement(evenementId);
        verifierModifiable(evenement);

        if (categorieRepository.existsByNomIgnoreCaseAndEvenementId(dto.nom().trim(), evenementId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette categorie existe deja pour cet evenement");
        }

        long total = categorieRepository.totalQuantite(evenementId) + dto.quantiteTotal();
        verifierCapacite(evenement, total);

        CategorieBillet categorie = categorieMapper.toEntity(dto);
        categorie.setEvenement(evenement);

        return categorieMapper.toResponse(categorieRepository.save(categorie));
    }

    // ---------- READ ----------
    @Transactional(readOnly = true)
    public List<CategorieBilletResponseDTO> findByEvenement(Long evenementId) {
        findEvenement(evenementId);
        return categorieMapper.toResponseList(categorieRepository.findByEvenementId(evenementId));
    }

    @Transactional(readOnly = true)
    public CategorieBilletResponseDTO findOne(Long evenementId, Long categorieId) {
        return categorieMapper.toResponse(findCategorie(evenementId, categorieId));
    }

    // ---------- UPDATE ----------
    @Transactional
    public CategorieBilletResponseDTO update(Long evenementId, Long categorieId, CategorieBilletRequestDTO dto) {

        CategorieBillet categorie = findCategorie(evenementId, categorieId);
        Evenement evenement = categorie.getEvenement();
        verifierModifiable(evenement);

        if (categorieRepository.existsByNomIgnoreCaseAndEvenementIdAndIdNot(dto.nom().trim(), evenementId, categorieId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette categorie existe deja pour cet evenement");
        }

        int utilisees = categorie.getPlacesReservees() + categorie.getPlacesVendues();
        if (dto.quantiteTotal() < utilisees) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La quantite totale ne peut pas etre inferieure aux places deja reservees ou vendues (" + utilisees + ")");
        }

        long autres = categorieRepository.totalQuantite(evenementId) - categorie.getQuantiteTotal();
        verifierCapacite(evenement, autres + dto.quantiteTotal());

        categorieMapper.updateEntity(dto, categorie);

        // no save() needed: the entity is managed
        return categorieMapper.toResponse(categorie);
    }

    // ---------- DELETE ----------
    @Transactional
    public void delete(Long evenementId, Long categorieId) {

        CategorieBillet categorie = findCategorie(evenementId, categorieId);

        if (categorie.getEvenement().getStatut() != StatutEvenement.BROUILLON) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une categorie ne peut etre supprimee que si l'evenement est en brouillon");
        }

        categorieRepository.delete(categorie);
    }

    // ---------- STOCK (used by ReservationService) ----------

    // SELECT ... FOR UPDATE: nobody else can change these categories until the commit
    @Transactional
    public Map<Long, CategorieBillet> verrouiller(Collection<Long> ids, Long evenementId) {
        return categorieRepository.findAllForUpdate(ids, evenementId).stream().collect(Collectors.toMap(CategorieBillet::getId, Function.identity()));
    }

    // checks the stock, then blocks the places
    @Transactional
    public void reserverPlaces(CategorieBillet categorie, int quantite) {
        if (quantite > categorie.placesDisponibles()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock insuffisant pour la categorie " + categorie.getNom() + " : " + categorie.placesDisponibles() + " place(s) disponible(s)");
        }
        categorie.setPlacesReservees(categorie.getPlacesReservees() + quantite);
    }

    // gives the blocked places back
    @Transactional
    public void libererPlaces(CategorieBillet categorie, int quantite) {
        categorie.setPlacesReservees(Math.max(0, categorie.getPlacesReservees() - quantite));
    }

    // reserved -> sold
    @Transactional
    public void confirmerVente(CategorieBillet categorie, int quantite) {
        categorie.setPlacesReservees(Math.max(0, categorie.getPlacesReservees() - quantite));
        categorie.setPlacesVendues(categorie.getPlacesVendues() + quantite);
    }

    // ---------- helpers ----------
    private Evenement findEvenement(Long evenementId) {
        return evenementRepository.findById(evenementId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evenement introuvable : " + evenementId));
    }

    private CategorieBillet findCategorie(Long evenementId, Long categorieId) {
        return categorieRepository.findByIdAndEvenementId(categorieId, evenementId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categorie " + categorieId + " introuvable pour l'evenement " + evenementId));
    }

    private void verifierModifiable(Evenement evenement) {
        if (evenement.getStatut() == StatutEvenement.ANNULE || evenement.getStatut() == StatutEvenement.TERMINE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un evenement annule ou termine ne peut plus etre modifie");
        }
    }

    private void verifierCapacite(Evenement evenement, long totalBillets) {
        if (totalBillets > evenement.getCapacite()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le total des billets (" + totalBillets + ") depasse la capacite de l'evenement (" + evenement.getCapacite() + ")");
        }
    }
}