package com.gestion.repository;

import com.gestion.persistent.enums.StatutExercice;
import com.gestion.persistent.model.ExerciceComptable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExerciceComptableRepository extends JpaRepository<ExerciceComptable, Long> {

    List<ExerciceComptable> findByPointDeVenteIdOrderByDateDebutDesc(Long pointDeVenteId);

    Optional<ExerciceComptable> findByCodeAndPointDeVenteId(String code, Long pointDeVenteId);

    @Query("SELECT e FROM ExerciceComptable e WHERE e.pointDeVenteId = :pointDeVenteId " +
           "AND :date BETWEEN e.dateDebut AND e.dateFin")
    Optional<ExerciceComptable> findByDateInExercice(
            @Param("date") LocalDate date,
            @Param("pointDeVenteId") Long pointDeVenteId);

    @Query("SELECT COUNT(e) > 0 FROM ExerciceComptable e WHERE e.pointDeVenteId = :pointDeVenteId " +
           "AND :date BETWEEN e.dateDebut AND e.dateFin AND e.statut = 'CLOTURE'")
    boolean isDateInExerciceCloture(
            @Param("date") LocalDate date,
            @Param("pointDeVenteId") Long pointDeVenteId);

    List<ExerciceComptable> findByPointDeVenteIdAndStatut(Long pointDeVenteId, StatutExercice statut);
}
