package com.eventpro.controller;

import com.eventpro.dto.user.*;
import com.eventpro.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * One single controller for the accounts of all actors, grouped under /api/users.
 * Who can call what (ADMIN only, CLIENT only...) is decided by the roles (Spring Security),
 * not by splitting controllers.
 */
@RestController
@RequestMapping("/api/users")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;
    private final ClientService clientService;
    private final OrganisateurService organisateurService;
    private final AdministrateurService administrateurService;
    private final AgentControleService agentControleService;

    public UtilisateurController(UtilisateurService utilisateurService, ClientService clientService, OrganisateurService organisateurService, AdministrateurService administrateurService, AgentControleService agentControleService) {
        this.utilisateurService = utilisateurService;
        this.clientService = clientService;
        this.organisateurService = organisateurService;
        this.administrateurService = administrateurService;
        this.agentControleService = agentControleService;
    }

    @GetMapping
    public ResponseEntity<List<UtilisateurResponseDTO>> findAll() {
        return ResponseEntity.ok(utilisateurService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UtilisateurResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(utilisateurService.findById(id));
    }

    @PatchMapping("/{id}/activer")
    public ResponseEntity<UtilisateurResponseDTO> activer(@PathVariable Long id) {
        return ResponseEntity.ok(utilisateurService.changerActivation(id, true));
    }

    @PatchMapping("/{id}/desactiver")
    public ResponseEntity<UtilisateurResponseDTO> desactiver(@PathVariable Long id) {
        return ResponseEntity.ok(utilisateurService.changerActivation(id, false));
    }

    @PostMapping("/clients")
    public ResponseEntity<UtilisateurResponseDTO> createClient(@Valid @RequestBody ClientRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(dto));
    }

    @GetMapping("/clients")
    public ResponseEntity<List<UtilisateurResponseDTO>> findAllClients() {
        return ResponseEntity.ok(clientService.findAll());
    }

    @GetMapping("/clients/{id}")
    public ResponseEntity<UtilisateurResponseDTO> findClient(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.findById(id));
    }

    @PutMapping("/clients/{id}")
    public ResponseEntity<UtilisateurResponseDTO> updateClient(@PathVariable Long id, @Valid @RequestBody ClientUpdateDTO dto) {
        return ResponseEntity.ok(clientService.update(id, dto));
    }

    @PostMapping("/organisateurs")
    public ResponseEntity<UtilisateurResponseDTO> createOrganisateur(@Valid @RequestBody OrganisateurRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organisateurService.create(dto));
    }

    @GetMapping("/organisateurs")
    public ResponseEntity<List<UtilisateurResponseDTO>> findAllOrganisateurs() {
        return ResponseEntity.ok(organisateurService.findAll());
    }

    @GetMapping("/organisateurs/{id}")
    public ResponseEntity<UtilisateurResponseDTO> findOrganisateur(@PathVariable Long id) {
        return ResponseEntity.ok(organisateurService.findById(id));
    }

    @PutMapping("/organisateurs/{id}")
    public ResponseEntity<UtilisateurResponseDTO> updateOrganisateur(@PathVariable Long id, @Valid @RequestBody OrganisateurUpdateDTO dto) {
        return ResponseEntity.ok(organisateurService.update(id, dto));
    }

    @DeleteMapping("/organisateurs/{id}")
    public ResponseEntity<Void> deleteOrganisateur(@PathVariable Long id) {
        organisateurService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/administrateurs")
    public ResponseEntity<UtilisateurResponseDTO> createAdministrateur(@Valid @RequestBody AdministrateurRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(administrateurService.create(dto));
    }

    @PostMapping("/agents")
    public ResponseEntity<UtilisateurResponseDTO> createAgent(@Valid @RequestBody AgentControleRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(agentControleService.create(dto));
    }
}
