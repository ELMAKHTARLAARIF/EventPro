package com.eventpro.controller;

import com.eventpro.dto.TicketResponseDTO;
import com.eventpro.service.TicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reservations/{reservationId}/billets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public ResponseEntity<List<TicketResponseDTO>> findByReservation(@PathVariable Long reservationId) {

        return ResponseEntity.ok(ticketService.findByReservation(reservationId));
    }
}