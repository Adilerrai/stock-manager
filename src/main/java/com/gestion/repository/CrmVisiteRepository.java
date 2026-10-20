package com.gestion.repository;

import com.gestion.persistent.enums.StatutVisite;
import com.gestion.persistent.model.CrmVisite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CrmVisiteRepository extends JpaRepository<CrmVisite, Long> {

    List<CrmVisite> findByPointDeVenteIdOrderByDateVisiteDesc(Long pointDeVenteId);

    @Query("SELECT v FROM CrmVisite v WHERE v.pointDeVenteId = :pointDeVenteId AND v.commercial.id = :commercialId ORDER BY v.dateVisite DESC")
    List<CrmVisite> findByPointDeVenteIdAndCommercialIdOrderByDateVisiteDesc(@Param("pointDeVenteId") Long pointDeVenteId, @Param("commercialId") Long commercialId);

    @Query("SELECT v FROM CrmVisite v WHERE v.pointDeVenteId = :pointDeVenteId AND v.entreprise.id = :entrepriseId ORDER BY v.dateVisite DESC")
    List<CrmVisite> findByPointDeVenteIdAndEntrepriseIdOrderByDateVisiteDesc(@Param("pointDeVenteId") Long pointDeVenteId, @Param("entrepriseId") Long entrepriseId);

    List<CrmVisite> findByPointDeVenteIdAndStatut(Long pointDeVenteId, StatutVisite statut);

    Optional<CrmVisite> findByIdAndPointDeVenteId(Long id, Long pointDeVenteId);

    // Visites planifiées sur une période
    @Query("SELECT v FROM CrmVisite v WHERE v.pointDeVenteId = :pointDeVenteId " +
           "AND v.dateVisite >= :debut AND v.dateVisite <= :fin ORDER BY v.dateVisite ASC")
    List<CrmVisite> findVisitesPeriode(@Param("pointDeVenteId") Long pointDeVenteId,
                                       @Param("debut") LocalDateTime debut,
                                       @Param("fin") LocalDateTime fin);

    // Visites planifiées par commercial
    @Query("SELECT v FROM CrmVisite v WHERE v.pointDeVenteId = :pointDeVenteId AND v.commercial.id = :commercialId " +
           "AND v.dateVisite >= :debut AND v.dateVisite <= :fin ORDER BY v.dateVisite ASC")
    List<CrmVisite> findVisitesCommercialPeriode(@Param("pointDeVenteId") Long pointDeVenteId,
                                                 @Param("commercialId") Long commercialId,
                                                 @Param("debut") LocalDateTime debut,
                                                 @Param("fin") LocalDateTime fin);

    long countByPointDeVenteId(Long pointDeVenteId);

    @Query("SELECT COUNT(v) FROM CrmVisite v WHERE v.pointDeVenteId = :pointDeVenteId AND v.commercial.id = :commercialId")
    long countByPointDeVenteIdAndCommercialId(@Param("pointDeVenteId") Long pointDeVenteId, @Param("commercialId") Long commercialId);

    @Query("SELECT COUNT(v) FROM CrmVisite v WHERE v.pointDeVenteId = :pointDeVenteId AND v.entreprise.id = :entrepriseId")
    long countByPointDeVenteIdAndEntrepriseId(@Param("pointDeVenteId") Long pointDeVenteId, @Param("entrepriseId") Long entrepriseId);

    @Query("SELECT COUNT(v) FROM CrmVisite v WHERE v.pointDeVenteId = :pointDeVenteId " +
           "AND v.statut = 'PLANIFIEE' AND v.dateVisite >= :debut AND v.dateVisite <= :fin")
    long countVisitesPlanifieesSemaine(@Param("pointDeVenteId") Long pointDeVenteId,
                                       @Param("debut") LocalDateTime debut,
                                       @Param("fin") LocalDateTime fin);

    @Query("SELECT COUNT(v) FROM CrmVisite v WHERE v.pointDeVenteId = :pointDeVenteId " +
           "AND (v.statut = 'REALISEE' OR v.statut = 'CLOTUREE') AND v.dateVisite >= :debut")
    long countVisitesRealiseesMois(@Param("pointDeVenteId") Long pointDeVenteId,
                                   @Param("debut") LocalDateTime debut);
}
