package com.eventpro.controller;

import com.eventpro.dto.EvenementAvecPromotionsDTO;
import com.eventpro.dto.PromotionRequestDTO;
import com.eventpro.dto.PromotionResponseDTO;
import com.eventpro.dto.PromotionUpdateDTO;
import com.eventpro.service.PromotionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    // ---------- events with promotions ----------
    @GetMapping("/evenements/avec-promotions")
    public ResponseEntity<List<EvenementAvecPromotionsDTO>> evenementsAvecPromotions() {
        return ResponseEntity.ok(promotionService.findEvenementsAvecPromotions());
    }

    // ---------- promotions of one event ----------
    @PostMapping("/evenements/{evenementId}/promotions")
    public ResponseEntity<PromotionResponseDTO> create(@PathVariable Long evenementId, @Valid @RequestBody PromotionRequestDTO dto) {

        return ResponseEntity.status(201).body(promotionService.create(evenementId, dto));
    }

    @GetMapping("/evenements/{evenementId}/promotions")
    public ResponseEntity<List<PromotionResponseDTO>> findByEvenement(@PathVariable Long evenementId) {

        return ResponseEntity.ok(promotionService.findByEvenement(evenementId));
    }

    @GetMapping("/evenements/{evenementId}/promotions/actives")
    public ResponseEntity<List<PromotionResponseDTO>> findActives(@PathVariable Long evenementId) {

        return ResponseEntity.ok(promotionService.findActives(evenementId));
    }

    @GetMapping("/evenements/{evenementId}/promotions/code/{code}")
    public ResponseEntity<PromotionResponseDTO> verifierCode(@PathVariable Long evenementId, @PathVariable String code) {

        return ResponseEntity.ok(promotionService.verifierCode(evenementId, code));
    }

    @GetMapping("/evenements/{evenementId}/promotions/{promotionId}")
    public ResponseEntity<PromotionResponseDTO> findOne(@PathVariable Long evenementId, @PathVariable Long promotionId) {

        return ResponseEntity.ok(promotionService.findOne(evenementId, promotionId));
    }

    @PutMapping("/evenements/{evenementId}/promotions/{promotionId}")
    public ResponseEntity<PromotionResponseDTO> update(@PathVariable Long evenementId, @PathVariable Long promotionId, @Valid @RequestBody PromotionUpdateDTO dto) {

        return ResponseEntity.ok(promotionService.update(evenementId, promotionId, dto));
    }

    @DeleteMapping("/evenements/{evenementId}/promotions/{promotionId}")
    public ResponseEntity<Void> delete(@PathVariable Long evenementId, @PathVariable Long promotionId) {

        promotionService.delete(evenementId, promotionId);
        return ResponseEntity.noContent().build();
    }

    // ---------- promotions of one organizer ----------
    @GetMapping("/organisateurs/{organisateurId}/promotions")
    public ResponseEntity<List<PromotionResponseDTO>> findByOrganisateur(@PathVariable Long organisateurId) {

        return ResponseEntity.ok(promotionService.findByOrganisateur(organisateurId));
    }
}