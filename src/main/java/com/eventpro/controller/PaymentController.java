package com.eventpro.controller;

import com.eventpro.dto.PaymentResponseDTO;
import com.eventpro.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/reservations/{reservationId}/paiement")
    public ResponseEntity<PaymentResponseDTO> findByReservation(@PathVariable Long reservationId) {
        return ResponseEntity.ok(paymentService.findByReservation(reservationId));
    }

    @GetMapping("/paiements/cle/{cleIdempotence}")
    public ResponseEntity<PaymentResponseDTO> findByCleIdempotence(@PathVariable String cleIdempotence) {
        return ResponseEntity.ok(paymentService.findByCleIdempotence(cleIdempotence));
    }
}