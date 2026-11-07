package com.eventpro.service;

import com.eventpro.dto.CheckInResponseDTO;
import com.eventpro.dto.egentController.CheckInRequestDTO;
import com.eventpro.entity.AgentControle;
import com.eventpro.entity.Billet;
import com.eventpro.enums.ResultatControle;
import com.eventpro.enums.StatutBillet;
import com.eventpro.mapper.CheckInMapper;
import com.eventpro.repository.AgentControleRepository;
import com.eventpro.repository.BilletRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CheckInService {

    private final BilletRepository billetRepository;
    private final AgentControleRepository agentRepository;
    private final CheckInMapper checkInMapper;

    public CheckInService(BilletRepository billetRepository, AgentControleRepository agentRepository, CheckInMapper checkInMapper) {
        this.billetRepository = billetRepository;
        this.agentRepository = agentRepository;
        this.checkInMapper = checkInMapper;
    }

    @Transactional
    public CheckInResponseDTO scanner(CheckInRequestDTO dto) {

        // only an active agent can scan (until Spring Security gives the logged-in user)
        AgentControle agent = agentRepository.findById(dto.agentId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent introuvable : " + dto.agentId()));
        if (!agent.isActif()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce compte est desactive");
        }

        // locked: a second agent scanning the same code waits for this transaction
        Billet billet = billetRepository.findByCodeForUpdate(normaliser(dto.code())).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Billet introuvable : " + dto.code()));

        Long evenementDuBillet = billet.getLigneReservation().getReservation().getEvenement().getId();

        ResultatControle resultat;
        String message;

        if (!evenementDuBillet.equals(dto.evenementId())) {
            resultat = ResultatControle.INVALIDE;
            message = "Ce billet n'est pas valable pour cet evenement";
        } else if (billet.controler()) {                     // VALIDE -> UTILISE, once only
            resultat = ResultatControle.VALIDE;
            message = "Acces autorise";
        } else if (billet.getStatut() == StatutBillet.UTILISE) {
            resultat = ResultatControle.DEJA_UTILISE;
            message = "Billet deja scanne le " + billet.getDateControle();
        } else {
            resultat = ResultatControle.INVALIDE;
            message = "Billet invalide (annule ou rembourse)";
        }

        // dateControle was set by controler() when the scan is accepted
        return checkInMapper.toResponse(billet, resultat, billet.getDateControle(), message);
    }

    // ---------- the agent only checks, nothing is composted ----------
    @Transactional(readOnly = true)
    public CheckInResponseDTO verifier(String code) {

        Billet billet = billetRepository.findByCode(normaliser(code)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Billet introuvable : " + code));

        ResultatControle resultat;
        String message;

        if (billet.valider()) {
            resultat = ResultatControle.VALIDE;
            message = "Billet valide, pas encore scanne";
        } else if (billet.getStatut() == StatutBillet.UTILISE) {
            resultat = ResultatControle.DEJA_UTILISE;
            message = "Billet deja scanne le " + billet.getDateControle();
        } else {
            resultat = ResultatControle.INVALIDE;
            message = "Billet invalide (annule ou rembourse)";
        }

        return checkInMapper.toResponse(billet, resultat, billet.getDateControle(), message);
    }

    private String normaliser(String code) {
        return code.trim().toUpperCase();
    }
}