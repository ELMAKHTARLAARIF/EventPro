package com.eventpro.repository;

import com.eventpro.entity.Evenement;
import com.eventpro.enums.StatutEvenement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EvenementRepository extends JpaRepository<Evenement, Long> {

    @Query("select distinct e from Evenement e join fetch e.promotions")
    List<Evenement> findAllWithPromotions();

    List<Evenement> findByOrganisateurIdOrderByDateDebutDesc(Long organisateurId);

    List<Evenement> findByStatutAndDateDebutAfterOrderByDateDebutAsc(StatutEvenement statut, LocalDateTime now);

    @Query("""
            select e from Evenement e
            where e.statut = :statut
              and :now between e.dateOuvertureVente and e.dateClotureVente
            order by e.dateDebut asc
            """)
    List<Evenement> findEnVente(@Param("statut") StatutEvenement statut, @Param("now") LocalDateTime now);

    boolean existsByOrganisateurIdAndTitreIgnoreCaseAndDateDebut(Long organisateurId, String titre, LocalDateTime dateDebut);

    boolean existsByOrganisateurId(Long organisateurId);



}