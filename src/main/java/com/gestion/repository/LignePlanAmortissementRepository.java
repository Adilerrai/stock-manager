package com.gestion.repository;

import com.gestion.persistent.model.Immobilisation;
import com.gestion.persistent.model.LignePlanAmortissement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LignePlanAmortissementRepository extends JpaRepository<LignePlanAmortissement, Long> {

    List<LignePlanAmortissement> findByImmobilisationOrderByAnneeAsc(Immobilisation immobilisation);

    @Query("SELECT l FROM LignePlanAmortissement l WHERE l.pointDeVenteId = :pointDeVenteId AND l.annee = :annee")
    List<LignePlanAmortissement> findByPointDeVenteIdAndAnnee(@Param("pointDeVenteId") Long pointDeVenteId, @Param("annee") Integer annee);

    @Query("SELECT l FROM LignePlanAmortissement l WHERE l.pointDeVenteId = :pointDeVenteId AND l.annee = :annee AND l.comptabilisee = false")
    List<LignePlanAmortissement> findNonComptabiliseesParAnnee(@Param("pointDeVenteId") Long pointDeVenteId, @Param("annee") Integer annee);
}
