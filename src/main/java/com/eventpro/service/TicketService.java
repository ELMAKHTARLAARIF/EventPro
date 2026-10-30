package com.eventpro.service;

import com.eventpro.dto.TicketResponseDTO;
import com.eventpro.entity.Billet;
import com.eventpro.entity.LigneReservation;
import com.eventpro.entity.Reservation;
import com.eventpro.enums.StatutBillet;
import com.eventpro.mapper.TicketMapper;
import com.eventpro.repository.BilletRepository;
import com.eventpro.repository.ReservationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TicketService {

    private final BilletRepository billetRepository;
    private final ReservationRepository reservationRepository;
    private final TicketMapper ticketMapper;

    public TicketService(BilletRepository billetRepository, ReservationRepository reservationRepository, TicketMapper ticketMapper) {
        this.billetRepository = billetRepository;
        this.reservationRepository = reservationRepository;
        this.ticketMapper = ticketMapper;
    }

    @Transactional
    public List<Billet> genererBillets(Reservation reservation) {

        List<Billet> billets = new ArrayList<>();

        for (LigneReservation ligne : reservation.getLignes()) {
            for (int i = 0; i < ligne.getQuantite(); i++) {
                Billet billet = new Billet();
                billet.setCode(genererCode());
                billet.setStatut(StatutBillet.VALIDE);
                billet.setLigneReservation(ligne);
                billets.add(billet);
            }
        }

        return billetRepository.saveAll(billets);
    }

    private String genererCode() {
        return "TKT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
    @Transactional(readOnly = true)
    public List<TicketResponseDTO> findByReservation(Long reservationId) {

        if (!reservationRepository.existsById(reservationId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation introuvable : " + reservationId);
        }

        return ticketMapper.toResponseList(billetRepository.findByLigneReservationReservationIdOrderById(reservationId));
    }
}