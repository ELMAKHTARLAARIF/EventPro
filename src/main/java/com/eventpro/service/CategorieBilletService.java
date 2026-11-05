package com.eventpro.service;

import com.eventpro.dto.category_billet.CategorieBilletRequestDTO;
import com.eventpro.dto.category_billet.CategorieBilletResponseDTO;
import com.eventpro.entity.CategorieBillet;
import com.eventpro.entity.Evenement;
import com.eventpro.enums.StatutEvenement;
// EXCEPTION: our custom business exceptions (handled by GlobalExceptionHandler)
import com.eventpro.exception.InsufficientStockException;
import com.eventpro.exception.ResourceNotFoundException;
import com.eventpro.exception.ValidationException;
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

    private final CategorieBilletRepository categorieBilletRepository;
    private final EvenementRepository evenementRepository;
    private final CategorieBilletMapper categorieMapper;

    public CategorieBilletService(CategorieBilletRepository categorieBilletRepository, EvenementRepository evenementRepository, CategorieBilletMapper categorieMapper) {
        this.categorieBilletRepository = categorieBilletRepository;
        this.evenementRepository = evenementRepository;
        this.categorieMapper = categorieMapper;
    }

    // ---------- CREATE ----------
    @Transactional
    public CategorieBilletResponseDTO create(Long evenementId, CategorieBilletRequestDTO dto) {

        Evenement evenement = findEvenement(evenementId);
        verifierModifiable(evenement);

        if (categorieBilletRepository.existsByNomIgnoreCaseAndEvenementId(dto.nom().trim(), evenementId)) {
            // EXCEPTION: generic 409 (duplicate): no dedicated exception, so ResponseStatusException stays
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette categorie existe deja pour cet evenement");
        }

        long total = categorieBilletRepository.totalQuantite(evenementId) + dto.quantiteTotal();
        verifierCapacite(evenement, total);

        CategorieBillet categorie = categorieMapper.toEntity(dto);
        categorie.setEvenement(evenement);

        return categorieMapper.toResponse(categorieBilletRepository.save(categorie));
    }

    // ---------- READ ----------
    @Transactional(readOnly = true)
    public List<CategorieBilletResponseDTO> findByEvenement(Long evenementId) {
        findEvenement(evenementId);
        return categorieMapper.toResponseList(categorieBilletRepository.findByEvenementId(evenementId));
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

        if (categorieBilletRepository.existsByNomIgnoreCaseAndEvenementIdAndIdNot(dto.nom().trim(), evenementId, categorieId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette categorie existe deja pour cet evenement");
        }

        int utilisees = categorie.getPlacesReservees() + categorie.getPlacesVendues();
        if (dto.quantiteTotal() < utilisees) {
            // EXCEPTION: invalid value sent by the client -> ValidationException (HTTP 400)
            throw new ValidationException("La quantite totale ne peut pas etre inferieure aux places deja reservees ou vendues (" + utilisees + ")");
        }

        long autres = categorieBilletRepository.totalQuantite(evenementId) - categorie.getQuantiteTotal();
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

        categorieBilletRepository.delete(categorie);
    }

    // ---------- STOCK (used by ReservationService) ----------

    // SELECT ... FOR UPDATE: nobody else can change these categories until the commit
    @Transactional
    public Map<Long, CategorieBillet> verrouiller(Collection<Long> ids, Long evenementId) {
        return categorieBilletRepository.findAllForUpdate(ids, evenementId).stream().collect(Collectors.toMap(CategorieBillet::getId, Function.identity()));
    }

    // checks the stock, then blocks the places
    @Transactional
    public void reserverPlaces(CategorieBillet categorie, int quantite) {
        if (quantite > categorie.placesDisponibles()) {
            // EXCEPTION: not enough free places -> InsufficientStockException (HTTP 409)
            throw new InsufficientStockException("Stock insuffisant pour la categorie " + categorie.getNom() + " : " + categorie.placesDisponibles() + " place(s) disponible(s)");
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
        // EXCEPTION: "not found" -> ResourceNotFoundException (HTTP 404)
        return evenementRepository.findById(evenementId).orElseThrow(() -> new ResourceNotFoundException("Evenement introuvable : " + evenementId));
    }

    private CategorieBillet findCategorie(Long evenementId, Long categorieId) {
        return categorieBilletRepository.findByIdAndEvenementId(categorieId, evenementId).orElseThrow(() -> new ResourceNotFoundException("Categorie " + categorieId + " introuvable pour l'evenement " + evenementId));
    }

    private void verifierModifiable(Evenement evenement) {
        if (evenement.getStatut() == StatutEvenement.ANNULE || evenement.getStatut() == StatutEvenement.TERMINE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un evenement annule ou termine ne peut plus etre modifie");
        }
    }

    private void verifierCapacite(Evenement evenement, long totalBillets) {
        if (totalBillets > evenement.getCapacite()) {
            throw new ValidationException("Le total des billets (" + totalBillets + ") depasse la capacite de l'evenement (" + evenement.getCapacite() + ")");
        }
    }







//    for remboursement

    // المقعد الذي استُردّت تكلفته لا يظلّ محجوزاً؛ بل يمكن بيعه مجدداً
    @Transactional
    public void annulerVente(CategorieBillet categorie, int quantite) {
        categorie.setPlacesVendues(Math.max(0, categorie.getPlacesVendues() - quantite));
    }
}