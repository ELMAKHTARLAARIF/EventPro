package com.eventpro.repository;

import com.eventpro.entity.Remboursement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RemboursementRepository extends JpaRepository<Remboursement, Long> {

    // paiement -> reservation -> id
    List<Remboursement> findByPaiementReservationIdOrderByDateDemandeDesc(Long reservationId);
}