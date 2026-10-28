package com.eventpro.service;

import com.eventpro.dto.EvenementAvecPromotionsDTO;
import com.eventpro.dto.PromotionRequestDTO;
import com.eventpro.dto.PromotionResponseDTO;
import com.eventpro.dto.PromotionUpdateDTO;
import com.eventpro.entity.Evenement;
import com.eventpro.entity.Promotion;
import com.eventpro.enums.StatutEvenement;
import com.eventpro.mapper.PromotionMapper;
import com.eventpro.repository.EvenementRepository;
import com.eventpro.repository.PromotionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final EvenementRepository evenementRepository;
    private final PromotionMapper promotionMapper;

    public PromotionService(PromotionRepository promotionRepository, EvenementRepository evenementRepository, PromotionMapper promotionMapper) {
        this.promotionRepository = promotionRepository;
        this.evenementRepository = evenementRepository;
        this.promotionMapper = promotionMapper;
    }

    // ---------- CREATE ----------
    @Transactional
    public PromotionResponseDTO create(Long evenementId, PromotionRequestDTO dto) {

        Evenement evenement = findEvenement(evenementId);

        if (evenement.getStatut() == StatutEvenement.ANNULE || evenement.getStatut() == StatutEvenement.TERMINE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Impossible d'ajouter une promotion a un evenement annule ou termine");
        }

        String code = dto.code().trim().toUpperCase();

        if (promotionRepository.existsByCodeIgnoreCaseAndEvenementId(code, evenementId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le code " + code + " existe deja pour cet evenement");
        }

        checkFinDansEvenement(dto.dateFin(), evenement);

        Promotion promotion = promotionMapper.toEntity(dto);   // DTO -> entity
        promotion.setEvenement(evenement);                     // relations: set here
        promotion.setOrganisateur(evenement.getOrganisateur());

        return promotionMapper.toResponse(promotionRepository.save(promotion));
    }

    // ---------- READ ----------
    @Transactional(readOnly = true)
    public List<PromotionResponseDTO> findByEvenement(Long evenementId) {
        findEvenement(evenementId);
        return promotionMapper.toResponseList(promotionRepository.findByEvenementId(evenementId));
    }

    @Transactional(readOnly = true)
    public PromotionResponseDTO findOne(Long evenementId, Long promotionId) {
        return promotionMapper.toResponse(findPromotion(evenementId, promotionId));
    }

    @Transactional(readOnly = true)
    public List<PromotionResponseDTO> findActives(Long evenementId) {
        findEvenement(evenementId);
        return promotionMapper.toResponseList(promotionRepository.findActives(evenementId, LocalDate.now()));
    }

    @Transactional(readOnly = true)
    public PromotionResponseDTO verifierCode(Long evenementId, String code) {
        Promotion promotion = promotionRepository.findByCodeIgnoreCaseAndEvenementId(code.trim(), evenementId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Code promo inconnu pour cet evenement"));

        if (!promotion.estUtilisable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce code promo n'est plus valide (expire ou limite atteinte)");
        }
        return promotionMapper.toResponse(promotion);
    }

    @Transactional(readOnly = true)
    public List<PromotionResponseDTO> findByOrganisateur(Long organisateurId) {
        return promotionMapper.toResponseList(promotionRepository.findByOrganisateurId(organisateurId));
    }

    @Transactional(readOnly = true)
    public List<EvenementAvecPromotionsDTO> findEvenementsAvecPromotions() {
        return promotionMapper.toEvenementAvecPromotionsList(evenementRepository.findAllWithPromotions());
    }

    // ---------- UPDATE ----------
    @Transactional
    public PromotionResponseDTO update(Long evenementId, Long promotionId, PromotionUpdateDTO dto) {

        Promotion promotion = findPromotion(evenementId, promotionId);

        String code = dto.code().trim().toUpperCase();

        if (promotionRepository.existsByCodeIgnoreCaseAndEvenementIdAndIdNot(code, evenementId, promotionId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le code " + code + " existe deja pour cet evenement");
        }

        if (dto.limiteUtilisation() < promotion.getNombreUtilisations()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La limite ne peut pas etre inferieure au nombre d'utilisations deja faites (" + promotion.getNombreUtilisations() + ")");
        }

        boolean dejaUtilisee = promotion.getNombreUtilisations() > 0;
        boolean reductionModifiee = dto.typeReduction() != promotion.getTypeReduction() || dto.valeurReduction().compareTo(promotion.getValeurReduction()) != 0;

        if (dejaUtilisee && reductionModifiee) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le type et la valeur de la reduction ne peuvent plus changer: la promotion a deja ete utilisee");
        }

        checkFinDansEvenement(dto.dateFin(), promotion.getEvenement());

        // all checks passed: now copy the new values into the managed entity
        promotionMapper.updateEntity(dto, promotion);

        // no save() needed: the entity is managed, Hibernate flushes the changes
        return promotionMapper.toResponse(promotion);
    }

    // ---------- DELETE ----------
    @Transactional
    public void delete(Long evenementId, Long promotionId) {

        Promotion promotion = findPromotion(evenementId, promotionId);

        if (promotion.getNombreUtilisations() > 0 || !promotion.getReservations().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Impossible de supprimer: cette promotion est deja utilisee par des reservations. " + "Modifiez plutot sa date de fin pour la terminer.");
        }

        promotionRepository.delete(promotion);
    }

    // ---------- helpers ----------
    private Evenement findEvenement(Long evenementId) {
        return evenementRepository.findById(evenementId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evenement introuvable : " + evenementId));
    }

    private Promotion findPromotion(Long evenementId, Long promotionId) {
        return promotionRepository.findByIdAndEvenementId(promotionId, evenementId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Promotion " + promotionId + " introuvable pour l'evenement " + evenementId));
    }

    private void checkFinDansEvenement(LocalDate dateFinPromo, Evenement evenement) {
        if (evenement.getDateFin() != null && dateFinPromo.isAfter(evenement.getDateFin().toLocalDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La promotion ne peut pas se terminer apres la fin de l'evenement");
        }
    }
}