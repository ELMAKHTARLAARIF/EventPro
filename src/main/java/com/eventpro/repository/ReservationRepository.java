package com.eventpro.repository;

import com.eventpro.entity.Reservation;
import com.eventpro.enums.StatutReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByEvenementIdOrderByDateCreationDesc(Long evenementId);

    // tickets this client already holds in this category (used for quotaMaxParClient)
    @Query("""
            select coalesce(sum(l.quantite), 0)
            from LigneReservation l
            where l.reservation.client.id = :clientId
              and l.categorie.id = :categorieId
              and l.reservation.statut in :statuts
            """)
    long quantiteDejaReservee(@Param("clientId") Long clientId, @Param("categorieId") Long categorieId, @Param("statuts") Collection<StatutReservation> statuts);
}