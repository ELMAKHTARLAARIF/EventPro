package com.eventpro.service;

import com.eventpro.dto.CategorieBilletRequestDTO;
import com.eventpro.dto.CategorieBilletResponseDTO;
import com.eventpro.entity.CategorieBillet;
import com.eventpro.entity.Evenement;
import com.eventpro.enums.StatutEvenement;
import com.eventpro.mapper.CategorieBilletMapper;
import com.eventpro.repository.CategorieBilletRepository;
import com.eventpro.repository.EvenementRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;

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

    @Transactional
    public CategorieBilletResponseDTO create(Long evenementId, CategorieBilletRequestDTO dto) {

        Evenement evenement = findEvenement(evenementId);
        verifierModifiable(evenement);

        if (categorieRepository.existsByNomIgnoreCaseAndEvenementId(dto.nom().trim(), evenementId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette categorie existe deja pour cet evenement");
        }
        long total = categorieRepository.totalQuantite(evenementId) + dto.quantiteTotal();
        verifierCapacite(evenement, total);

        CategorieBillet categorieBillet = categorieMapper.toEntity(dto);
        categorieBillet.setEvenement(evenement);
        categorieRepository.save(categorieBillet);

        return categorieMapper.toResponse(categorieBillet);

    }

    @Transactional(readOnly = true)
    public CategorieBilletResponseDTO findByEvenement(Long evenementId) {
        findEvenement(evenementId);
        return categorieMapper.toResponse(categorieRepository.findById(evenementId).get());

    }

    @Transactional(readOnly = true)
    public CategorieBilletResponseDTO findOne(Long evenementId, Long categoryBilletId) {
        return categorieMapper.toResponse(findCategorie(evenementId, categoryBilletId));
    }

    public CategorieBilletResponseDTO update(Long evenementId, Long categoryBilletId, @Valid @RequestBody CategorieBilletRequestDTO dto) {
        CategorieBillet categorieBillet = findCategorie(evenementId, categoryBilletId);
        Evenement evenement = categorieBillet.getEvenement();
        verifierModifiable(evenement);
        if (categorieRepository.existsByNomIgnoreCaseAndEvenementIdAndIdNot(dto.nom().trim(), evenementId, categoryBilletId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette categorie existe deja pour cet identifiant");
        }
        int utilisees = categorieBillet.getPlacesReservees() + categorieBillet.getPlacesVendues();
        if (dto.quantiteTotal() < utilisees) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La quantite totale ne peut pas etre inferieure aux places deja reservees ou vendues (" + utilisees + ")");
        }
        long autres = categorieRepository.totalQuantite(evenementId) - categorieBillet.getQuantiteTotal(); // quantite total 9dima - quantite dyal categorieBillet li ghadi n updatew bch n9dro n calculew Q jdida
        verifierCapacite(evenement, autres + dto.quantiteTotal()); // dto.quantiteTotal the new  Quantite dyal hd CategorieBillet
        categorieMapper.updateEntity(dto, categorieBillet);
        return categorieMapper.toResponse(categorieBillet);

    }

    @Transactional
    public void delete(Long evenementId, Long categorieId) {

        CategorieBillet categorie = findCategorie(evenementId, categorieId);

        if (categorie.getEvenement().getStatut() != StatutEvenement.BROUILLON) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une categorie ne peut etre supprimee que si l'evenement est en brouillon");
        }

        categorieRepository.delete(categorie);
    }

    // ---------- helpers ----------
    private Evenement findEvenement(Long evenementId) {
        return evenementRepository.findById(evenementId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evenement introuvable : " + evenementId));
    }

    private CategorieBillet findCategorie(Long evenementId, Long categoryBilletId) {
        return categorieRepository.findByIdAndEvenementId(categoryBilletId, evenementId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categorie " + categoryBilletId + " introuvable pour l'evenement " + evenementId));
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