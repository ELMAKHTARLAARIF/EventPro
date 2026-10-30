package com.eventpro.repository;

import com.eventpro.entity.Reservation;
import com.eventpro.enums.StatutReservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.id = :id")
    Optional<Reservation> findByIdForUpdate(@Param("id") Long id);

    // tickets this client already holds in this category, ignoring one reservation
    @Query("""
            select coalesce(sum(l.quantite), 0)
            from LigneReservation l
            where l.reservation.client.id = :clientId
              and l.categorie.id = :categorieId
              and l.reservation.statut in :statuts
              and l.reservation.id <> :excludeId
            """)
    long quantiteDejaReservee(@Param("clientId") Long clientId, @Param("categorieId") Long categorieId, @Param("statuts") Collection<StatutReservation> statuts, @Param("excludeId") Long excludeId);
}