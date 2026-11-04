package com.eventpro.service;

import com.eventpro.dto.Remboursement.RemboursementRequestDTO;
import com.eventpro.dto.Remboursement.RemboursementResponseDTO;
import com.eventpro.entity.*;
import com.eventpro.enums.StatutPaiement;
import com.eventpro.enums.StatutReservation;
import com.eventpro.mapper.RemboursementMapper;
import com.eventpro.repository.RemboursementRepository;
import com.eventpro.repository.ReservationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class RemboursementService {

    private static final long DELAI_REMBOURSEMENT_HEURES = 48;   // 48h

    private final ReservationRepository reservationRepository;
    private final RemboursementRepository remboursementRepository;
    private final CategorieBilletService categorieBilletService;
    private final TicketService ticketService;
    private final RemboursementMapper remboursementMapper;

    public RemboursementService(ReservationRepository reservationRepository, RemboursementRepository remboursementRepository, CategorieBilletService categorieBilletService, TicketService ticketService, RemboursementMapper remboursementMapper) {
        this.reservationRepository = reservationRepository;
        this.remboursementRepository = remboursementRepository;
        this.categorieBilletService = categorieBilletService;
        this.ticketService = ticketService;
        this.remboursementMapper = remboursementMapper;
    }

    @Transactional
    public RemboursementResponseDTO demander(Long reservationId, RemboursementRequestDTO dto) {

        Reservation reservation = reservationRepository.findByIdForUpdate(reservationId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation introuvable : " + reservationId));

        if (reservation.getStatut() != StatutReservation.CONFIRMEE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seule une reservation payee et confirmee peut etre remboursee");
        }

        Paiement paiement = reservation.getPaiement();
        if (paiement == null || paiement.getStatut() != StatutPaiement.REUSSI) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Aucun paiement reussi pour cette reservation");
        }

        Remboursement remboursement = nouveau(paiement, dto.motif().trim());

        // 48h or more before the event: 100%. Less than 48h: refusal.
        LocalDateTime limite = reservation.getEvenement().getDateDebut().minusHours(DELAI_REMBOURSEMENT_HEURES);

        if (LocalDateTime.now().isAfter(limite)) {
            remboursement.setMontant(BigDecimal.ZERO);
            remboursement.refuser();                     // tickets stay VALIDE
        } else {
            remboursement.setMontant(paiement.getMontant());
            appliquer(reservation, remboursement);       // full refund
        }

        return remboursementMapper.toResponse(remboursementRepository.save(remboursement));
    }

    @Transactional
    public void rembourserEvenementAnnule(Evenement evenement) {

        List<Reservation> reservations = reservationRepository.findByEvenementAndStatutForUpdate(evenement.getId(), StatutReservation.CONFIRMEE);

        for (Reservation reservation : reservations) {
            Paiement paiement = reservation.getPaiement();

            Remboursement remboursement = nouveau(paiement, "Evenement annule par l'organisateur");
            remboursement.setMontant(paiement.getMontant());
            appliquer(reservation, remboursement);

            remboursementRepository.save(remboursement);
        }
    }

    // ---------- READ ----------
    @Transactional(readOnly = true)
    public List<RemboursementResponseDTO> findByReservation(Long reservationId) {

        if (!reservationRepository.existsById(reservationId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation introuvable : " + reservationId);
        }

        return remboursementMapper.toResponseList(remboursementRepository.findByPaiementReservationIdOrderByDateDemandeDesc(reservationId));
    }

    // ---------- helpers ----------

    // places sellable again, tickets cancelled, reservation refunded, refund approved
    private void appliquer(Reservation reservation, Remboursement remboursement) {

        List<Long> ids = reservation.getLignes().stream().map(l -> l.getCategorie().getId()).distinct().toList();

        Map<Long, CategorieBillet> categories = categorieBilletService.verrouiller(ids, reservation.getEvenement().getId());

        for (LigneReservation ligne : reservation.getLignes()) {
            categorieBilletService.annulerVente(categories.get(ligne.getCategorie().getId()), ligne.getQuantite());
        }

        ticketService.annulerBillets(reservation);
        reservation.setStatut(StatutReservation.REMBOURSEE);
        remboursement.approuver();
    }

    private Remboursement nouveau(Paiement paiement, String motif) {
        Remboursement remboursement = new Remboursement();
        remboursement.setPaiement(paiement);
        remboursement.setMotif(motif);
        remboursement.setDateDemande(LocalDateTime.now());
        return remboursement;
    }
}