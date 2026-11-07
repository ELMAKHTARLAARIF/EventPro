package com.eventpro.controller;

import com.eventpro.dto.CheckInResponseDTO;
import com.eventpro.dto.egentController.CheckInRequestDTO;
import com.eventpro.enums.ResultatControle;
import com.eventpro.service.CheckInService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkin")
public class CheckInController {

    private final CheckInService checkInService;

    public CheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    // 200 = access granted, 409 = refused (already used or invalid)
    @PostMapping
    public ResponseEntity<CheckInResponseDTO> scanner(@Valid @RequestBody CheckInRequestDTO dto) {

        CheckInResponseDTO resultat = checkInService.scanner(dto);

        HttpStatus status = resultat.resultat() == ResultatControle.VALIDE ? HttpStatus.OK : HttpStatus.CONFLICT;

        return ResponseEntity.status(status).body(resultat);
    }

    @GetMapping("/{code}")
    public ResponseEntity<CheckInResponseDTO> verifier(@PathVariable String code) {
        return ResponseEntity.ok(checkInService.verifier(code));
    }
}