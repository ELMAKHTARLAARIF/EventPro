package com.eventpro.repository;

import com.eventpro.entity.Paiement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaiementRepository extends JpaRepository<Paiement, Long> {

    Optional<Paiement> findByCleIdempotence(String cleIdempotence);

    Optional<Paiement> findByReservationId(Long reservationId);
}