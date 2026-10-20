package com.gestion.repository;

import com.gestion.persistent.enums.StatutTache;
import com.gestion.persistent.model.CrmTache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CrmTacheRepository extends JpaRepository<CrmTache, Long> {

    List<CrmTache> findByPointDeVenteIdOrderByDateEcheanceAsc(Long pointDeVenteId);

    @Query("SELECT t FROM CrmTache t WHERE t.pointDeVenteId = :pointDeVenteId AND t.assigneA.id = :userId ORDER BY t.dateEcheance ASC")
    List<CrmTache> findByPointDeVenteIdAndAssigneAIdOrderByDateEcheanceAsc(@Param("pointDeVenteId") Long pointDeVenteId, @Param("userId") Long userId);

    @Query("SELECT t FROM CrmTache t WHERE t.pointDeVenteId = :pointDeVenteId AND t.assigneA.id = :userId AND t.statut = :statut")
    List<CrmTache> findByPointDeVenteIdAndAssigneAIdAndStatut(@Param("pointDeVenteId") Long pointDeVenteId, @Param("userId") Long userId, @Param("statut") StatutTache statut);

    @Query("SELECT t FROM CrmTache t WHERE t.pointDeVenteId = :pointDeVenteId AND t.rapport.id = :rapportId")
    List<CrmTache> findByPointDeVenteIdAndRapportId(@Param("pointDeVenteId") Long pointDeVenteId, @Param("rapportId") Long rapportId);

    @Query("SELECT t FROM CrmTache t WHERE t.pointDeVenteId = :pointDeVenteId AND t.entreprise.id = :entrepriseId")
    List<CrmTache> findByPointDeVenteIdAndEntrepriseId(@Param("pointDeVenteId") Long pointDeVenteId, @Param("entrepriseId") Long entrepriseId);

    Optional<CrmTache> findByIdAndPointDeVenteId(Long id, Long pointDeVenteId);

    // Tâches en retard (statut A_FAIRE ou EN_COURS et dateEcheance < today)
    @Query("SELECT t FROM CrmTache t WHERE t.pointDeVenteId = :pointDeVenteId " +
           "AND t.statut IN ('A_FAIRE', 'EN_COURS') AND t.dateEcheance < :today ORDER BY t.dateEcheance ASC")
    List<CrmTache> findTachesEnRetard(@Param("pointDeVenteId") Long pointDeVenteId, @Param("today") LocalDate today);

    @Query("SELECT t FROM CrmTache t WHERE t.pointDeVenteId = :pointDeVenteId AND t.assigneA.id = :userId " +
           "AND t.statut IN ('A_FAIRE', 'EN_COURS') AND t.dateEcheance < :today ORDER BY t.dateEcheance ASC")
    List<CrmTache> findTachesUserEnRetard(@Param("pointDeVenteId") Long pointDeVenteId,
                                         @Param("userId") Long userId,
                                         @Param("today") LocalDate today);

    long countByPointDeVenteIdAndStatut(Long pointDeVenteId, StatutTache statut);

    @Query("SELECT COUNT(t) FROM CrmTache t WHERE t.pointDeVenteId = :pointDeVenteId " +
           "AND t.statut IN ('A_FAIRE', 'EN_COURS') AND t.dateEcheance < :today")
    long countTachesEnRetard(@Param("pointDeVenteId") Long pointDeVenteId, @Param("today") LocalDate today);

    @Query("SELECT COUNT(t) FROM CrmTache t WHERE t.pointDeVenteId = :pointDeVenteId AND t.assigneA.id = :userId " +
           "AND t.statut IN ('A_FAIRE', 'EN_COURS') AND t.dateEcheance < :today")
    long countTachesUserEnRetard(@Param("pointDeVenteId") Long pointDeVenteId,
                                 @Param("userId") Long userId,
                                 @Param("today") LocalDate today);
}
