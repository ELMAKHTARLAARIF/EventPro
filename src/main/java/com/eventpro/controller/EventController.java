package com.eventpro.controller;

import com.eventpro.dto.EventRequestDTO;
import com.eventpro.dto.EventResponseDTO;
import com.eventpro.enums.StatutEvenement;
import com.eventpro.service.EventService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping("/evenements")
    public ResponseEntity<EventResponseDTO> create(@Valid @RequestBody EventRequestDTO dto) {
        return ResponseEntity.status(201).body(eventService.create(dto));
    }

    @GetMapping("/evenements")
    public ResponseEntity<Page<EventResponseDTO>> search(@RequestParam(required = false) StatutEvenement statut, @RequestParam(required = false) String q, @PageableDefault(size = 10, sort = "dateDebut") Pageable pageable) {

        return ResponseEntity.ok(eventService.search(statut, q, pageable));
    }

    @GetMapping("/evenements/a-venir")
    public ResponseEntity<List<EventResponseDTO>> aVenir() {
        return ResponseEntity.ok(eventService.findAVenir());
    }

    @GetMapping("/evenements/en-vente")
    public ResponseEntity<List<EventResponseDTO>> enVente() {
        return ResponseEntity.ok(eventService.findEnVente());
    }

    @GetMapping("/evenements/{id}")
    public ResponseEntity<EventResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.findById(id));
    }

    @GetMapping("/organisateurs/{organisateurId}/evenements")
    public ResponseEntity<List<EventResponseDTO>> findByOrganisateur(@PathVariable Long organisateurId) {

        return ResponseEntity.ok(eventService.findByOrganisateur(organisateurId));
    }

    @PutMapping("/evenements/{id}")
    public ResponseEntity<EventResponseDTO> update(@PathVariable Long id, @Valid @RequestBody EventRequestDTO dto) {

        return ResponseEntity.ok(eventService.update(id, dto));
    }

    @PatchMapping("/evenements/{id}/publier")
    public ResponseEntity<EventResponseDTO> publier(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.publier(id));
    }

    @PatchMapping("/evenements/{id}/annuler")
    public ResponseEntity<EventResponseDTO> annuler(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.annuler(id));
    }

    @DeleteMapping("/evenements/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        eventService.delete(id);
        return ResponseEntity.noContent().build();
    }
}