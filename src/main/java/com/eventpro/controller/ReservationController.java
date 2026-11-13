package com.eventpro.controller;

import com.eventpro.dto.reservation.ReservationRequestDTO;
import com.eventpro.dto.reservation.ReservationResponseDTO;
import com.eventpro.dto.reservation.ReservationUpdateDTO;
import com.eventpro.enums.StatutReservation;
import com.eventpro.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/reservations")
    public ResponseEntity<ReservationResponseDTO> create(@Valid @RequestBody ReservationRequestDTO dto) {

        return ResponseEntity.status(201).body(reservationService.create(dto));
    }

    @GetMapping("/evenements/{evenementId}/reservations")
    public ResponseEntity<List<ReservationResponseDTO>> findAllByEvenement(@PathVariable Long evenementId) {

        return ResponseEntity.ok(reservationService.findAllByEvenementId(evenementId));
    }

    @PutMapping("/reservations/{id}")
    public ResponseEntity<ReservationResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody ReservationUpdateDTO dto) {

        return ResponseEntity.ok(reservationService.update(id, dto));
    }


    // Misse en Situation (reservationService -> reservationRepository )
    @GetMapping("/api/v1/reservations/recherche")
    public ResponseEntity<List<ReservationResponseDTO>> findClientsWithReservationConfirme(@PathVariable("evenementId") Long evenementId, @RequestParam StatutReservation statutReservation, String email,String nomCategorie,Long quantiteMin) {

        return ResponseEntity.ok(reservationService.getClientWithReservationSConfirme(evenementId,statutReservation,email,nomCategorie,quantiteMin));
    }
}