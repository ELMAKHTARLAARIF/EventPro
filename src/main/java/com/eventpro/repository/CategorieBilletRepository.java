package com.eventpro.repository;

import com.eventpro.entity.CategorieBillet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CategorieBilletRepository extends JpaRepository<CategorieBillet, Long> {

    List<CategorieBillet> findByEvenementId(Long evenementId);

    // makes sure the category belongs to this event
    Optional<CategorieBillet> findByIdAndEvenementId(Long id, Long evenementId);

    boolean existsByNomIgnoreCaseAndEvenementId(String nom, Long evenementId);

    boolean existsByNomIgnoreCaseAndEvenementIdAndIdNot(String nom, Long evenementId, Long id);

    // total tickets already defined for this event (0 if none)
    @Query("select coalesce(sum(c.quantiteTotal), 0) from CategorieBillet c where c.evenement.id = :evenementId")
    long totalQuantite(@Param("evenementId") Long evenementId);

    // SELECT ... FOR UPDATE: no other transaction can touch these rows until we commit
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select c from CategorieBillet c
        where c.id in :ids and c.evenement.id = :evenementId
        order by c.id
        """)
    List<CategorieBillet> findAllForUpdate(@Param("ids") Collection<Long> ids,
                                           @Param("evenementId") Long evenementId);
}