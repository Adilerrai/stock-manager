package com.gestion.repository;

import com.gestion.persistent.model.LieuLogistique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LieuLogistiqueRepository extends JpaRepository<LieuLogistique, Long> {

    @Query("SELECT l FROM LieuLogistique l WHERE l.actif = true AND (l.pointDeVenteId IS NULL OR l.pointDeVenteId = :pointDeVenteId) ORDER BY l.libelle ASC")
    List<LieuLogistique> findActifsForTenant(@Param("pointDeVenteId") Long pointDeVenteId);

    Optional<LieuLogistique> findByCodeAndPointDeVenteId(String code, Long pointDeVenteId);

    Optional<LieuLogistique> findByCodeAndPointDeVenteIdIsNull(String code);
}
