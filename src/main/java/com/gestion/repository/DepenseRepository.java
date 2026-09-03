package com.gestion.repository;

import com.gestion.persistent.enums.CategorieDepense;
import com.gestion.persistent.model.Depense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DepenseRepository extends JpaRepository<Depense, Long> {

    Optional<Depense> findByReference(String reference);

    List<Depense> findByPointDeVenteIdOrderByDateDepenseDesc(Long pointDeVenteId);

    @Query("SELECT d FROM Depense d WHERE d.dateDepense BETWEEN :debut AND :fin ORDER BY d.dateDepense DESC")
    List<Depense> findByPeriode(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    @Query("SELECT d FROM Depense d WHERE d.categorie = :categorie AND d.dateDepense BETWEEN :debut AND :fin ORDER BY d.dateDepense DESC")
    List<Depense> findByCategorieAndPeriode(@Param("categorie") CategorieDepense categorie,
                                           @Param("debut") LocalDate debut,
                                           @Param("fin") LocalDate fin);

    @Query("SELECT COALESCE(SUM(d.montant), 0) FROM Depense d WHERE d.dateDepense BETWEEN :debut AND :fin")
    BigDecimal sumMontantByPeriode(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    @Query("SELECT d.categorie, COALESCE(SUM(d.montant), 0) FROM Depense d WHERE d.dateDepense BETWEEN :debut AND :fin GROUP BY d.categorie ORDER BY SUM(d.montant) DESC")
    List<Object[]> findTotauxParCategorie(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);
}
