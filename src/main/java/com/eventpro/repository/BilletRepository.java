package com.eventpro.repository;

import com.eventpro.entity.Billet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BilletRepository extends JpaRepository<Billet, Long> {

    // Billet -> ligneReservation -> reservation -> id
    List<Billet> findByLigneReservationReservationIdOrderById(Long reservationId);


    Optional<Billet> findByCode(String code);

    // SELECT ... FOR UPDATE: two agents scanning the same code at the same moment
    // are served one after the other, so only one of them can compost the ticket
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Billet b where b.code = :code")
    Optional<Billet> findByCodeForUpdate(@Param("code") String code);
}