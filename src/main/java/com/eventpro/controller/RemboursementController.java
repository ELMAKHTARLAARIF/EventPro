package com.eventpro.controller;

import com.eventpro.dto.Remboursement.RemboursementRequestDTO;
import com.eventpro.dto.Remboursement.RemboursementResponseDTO;
import com.eventpro.service.RemboursementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations/{reservationId}/remboursements")
public class RemboursementController {

    private final RemboursementService remboursementService;

    public RemboursementController(RemboursementService remboursementService) {
        this.remboursementService = remboursementService;
    }

    @PostMapping
    public ResponseEntity<RemboursementResponseDTO> demander(@PathVariable Long reservationId, @Valid @RequestBody RemboursementRequestDTO dto) {

        return ResponseEntity.status(201).body(remboursementService.demander(reservationId, dto));
    }

    @GetMapping
    public ResponseEntity<List<RemboursementResponseDTO>> findByReservation(@PathVariable Long reservationId) {

        return ResponseEntity.ok(remboursementService.findByReservation(reservationId));
    }
}