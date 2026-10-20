package com.gestion.repository;

import com.gestion.persistent.enums.StatutEntreprise;
import com.gestion.persistent.model.CrmEntreprise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CrmEntrepriseRepository extends JpaRepository<CrmEntreprise, Long> {

    List<CrmEntreprise> findByPointDeVenteIdOrderByNomAsc(Long pointDeVenteId);

    List<CrmEntreprise> findByPointDeVenteIdAndStatut(Long pointDeVenteId, StatutEntreprise statut);

    List<CrmEntreprise> findByPointDeVenteIdAndCommercialResponsableId(Long pointDeVenteId, Long commercialId);

    Optional<CrmEntreprise> findByIdAndPointDeVenteId(Long id, Long pointDeVenteId);

    @Query("SELECT e FROM CrmEntreprise e WHERE e.pointDeVenteId = :pointDeVenteId AND " +
           "(LOWER(e.nom) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(e.telephone) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(e.ice) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(e.ville) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<CrmEntreprise> searchEntreprises(@Param("query") String query, @Param("pointDeVenteId") Long pointDeVenteId);

    // Détection de doublons (par nom ou ICE dans le même tenant)
    @Query("SELECT e FROM CrmEntreprise e WHERE e.pointDeVenteId = :pointDeVenteId AND " +
           "(LOWER(TRIM(e.nom)) = LOWER(TRIM(:nom)) " +
           "OR (:ice IS NOT NULL AND :ice <> '' AND e.ice = :ice))")
    List<CrmEntreprise> findDoublonsPotentiels(@Param("nom") String nom, @Param("ice") String ice, @Param("pointDeVenteId") Long pointDeVenteId);

    long countByPointDeVenteId(Long pointDeVenteId);

    long countByPointDeVenteIdAndCommercialResponsableId(Long pointDeVenteId, Long commercialId);
}
