package com.gestion.repository;

import com.gestion.persistent.model.ConditionPaiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConditionPaiementRepository extends JpaRepository<ConditionPaiement, Long> {

    @Query("SELECT c FROM ConditionPaiement c LEFT JOIN FETCH c.lignes WHERE c.actif = true AND (c.pointDeVenteId IS NULL OR c.pointDeVenteId = :pointDeVenteId) ORDER BY c.libelle ASC")
    List<ConditionPaiement> findActifsForTenant(@Param("pointDeVenteId") Long pointDeVenteId);

    Optional<ConditionPaiement> findByCodeAndPointDeVenteId(String code, Long pointDeVenteId);

    Optional<ConditionPaiement> findByCodeAndPointDeVenteIdIsNull(String code);
}
