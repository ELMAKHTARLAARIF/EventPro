package com.eventpro.repository;

import com.eventpro.entity.Promotion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    List<Promotion> findByEvenementId(Long evenementId);


    boolean existsByCodeIgnoreCaseAndEvenementId(String code, Long evenementId);

    // used by update: same code is allowed on the promotion itself
    boolean existsByCodeIgnoreCaseAndEvenementIdAndIdNot(String code, Long evenementId, Long id);

    List<Promotion> findByOrganisateurId(Long organisateurId);

    // guarantees the promotion belongs to this event
    Optional<Promotion> findByIdAndEvenementId(Long id, Long evenementId);

    Optional<Promotion> findByCodeIgnoreCaseAndEvenementId(String code, Long evenementId);

    @Query("""
            select p from Promotion p
            where p.evenement.id = :evenementId
              and :today between p.dateDebut and p.dateFin
              and p.nombreUtilisations < p.limiteUtilisation
            """)
    List<Promotion> findActives(@Param("evenementId") Long evenementId, @Param("today") LocalDate today);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Promotion> findWithLockByCodeIgnoreCaseAndEvenementId(String code, Long evenementId);
}
