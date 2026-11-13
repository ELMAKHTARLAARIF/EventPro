package com.eventpro.controller;

import com.eventpro.dto.category_billet.CategorieBilletRequestDTO;
import com.eventpro.dto.category_billet.CategorieBilletResponseDTO;
import com.eventpro.service.CategorieBilletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evenements/{evenementId}/categories")
public class CategorieBilletController {

    private final CategorieBilletService categorieService;

    public CategorieBilletController(CategorieBilletService categorieService) {
        this.categorieService = categorieService;
    }

    @PostMapping
    public ResponseEntity<CategorieBilletResponseDTO> create(@PathVariable Long evenementId, @Valid @RequestBody CategorieBilletRequestDTO dto) {

        return ResponseEntity.status(201).body(categorieService.create(evenementId, dto));
    }
    @GetMapping
    public ResponseEntity<List<CategorieBilletResponseDTO>> findByEvenement(@PathVariable Long evenementId) {
        return ResponseEntity.ok(categorieService.findByEvenement(evenementId));
    }
    @GetMapping("/{categorieId}")
    public ResponseEntity<CategorieBilletResponseDTO> findOne(@PathVariable Long evenementId, @PathVariable Long categoryBilletId) {
        return ResponseEntity.ok(categorieService.findOne(evenementId, categoryBilletId));
    }
    @PutMapping("/{categorieId}")
    public ResponseEntity<CategorieBilletResponseDTO> update(@PathVariable Long categoryBilletId, @PathVariable Long evenementId, @Valid @RequestBody CategorieBilletRequestDTO dto) {

        return ResponseEntity.ok(categorieService.update(evenementId, categoryBilletId, dto));

    }

    @DeleteMapping("/{categorieId}")
    public ResponseEntity<Void> delete(@PathVariable Long evenementId, @PathVariable Long categorieId) {

        categorieService.delete(evenementId, categorieId);
        return ResponseEntity.noContent().build();
    }
}