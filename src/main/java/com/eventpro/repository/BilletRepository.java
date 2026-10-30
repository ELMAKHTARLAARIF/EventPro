package com.eventpro.repository;

import com.eventpro.entity.Billet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BilletRepository extends JpaRepository<Billet, Long> {

    // Billet -> ligneReservation -> reservation -> id
    List<Billet> findByLigneReservationReservationIdOrderById(Long reservationId);
}