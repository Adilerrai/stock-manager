package com.gestion.repository;

import com.gestion.persistent.enums.StatutRapportVisite;
import com.gestion.persistent.model.CrmRapportVisite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CrmRapportVisiteRepository extends JpaRepository<CrmRapportVisite, Long> {

    List<CrmRapportVisite> findByPointDeVenteIdOrderByDateCreationDesc(Long pointDeVenteId);

    @Query("SELECT r FROM CrmRapportVisite r WHERE r.visite.id = :visiteId")
    Optional<CrmRapportVisite> findByVisiteId(@Param("visiteId") Long visiteId);

    Optional<CrmRapportVisite> findByIdAndPointDeVenteId(Long id, Long pointDeVenteId);

    List<CrmRapportVisite> findByPointDeVenteIdAndStatut(Long pointDeVenteId, StatutRapportVisite statut);

    // Rapports soumis en attente de validation pour le manager
    @Query("SELECT r FROM CrmRapportVisite r WHERE r.pointDeVenteId = :pointDeVenteId " +
           "AND r.statut = 'SOUMIS' ORDER BY r.soumisLe ASC")
    List<CrmRapportVisite> findRapportsEnAttenteValidation(@Param("pointDeVenteId") Long pointDeVenteId);

    // Rapports d'un commercial spécifique
    @Query("SELECT r FROM CrmRapportVisite r WHERE r.pointDeVenteId = :pointDeVenteId " +
           "AND r.visite.commercial.id = :commercialId ORDER BY r.dateCreation DESC")
    List<CrmRapportVisite> findByCommercialId(@Param("pointDeVenteId") Long pointDeVenteId,
                                              @Param("commercialId") Long commercialId);

    // Compteurs pour dashboard
    long countByPointDeVenteIdAndStatut(Long pointDeVenteId, StatutRapportVisite statut);

    @Query("SELECT COUNT(r) FROM CrmRapportVisite r WHERE r.pointDeVenteId = :pointDeVenteId " +
           "AND r.statut = 'SOUMIS'")
    long countRapportsEnAttenteValidation(@Param("pointDeVenteId") Long pointDeVenteId);

    // Rapports en retard (visite réalisée depuis plus de 24h avec rapport encore brouillon ou inexistant)
    @Query("SELECT COUNT(v) FROM CrmVisite v LEFT JOIN CrmRapportVisite r ON r.visite.id = v.id " +
           "WHERE v.pointDeVenteId = :pointDeVenteId AND v.statut = 'REALISEE' " +
           "AND v.dateVisite <= :seuil24h AND (r.id IS NULL OR r.statut = 'BROUILLON')")
    long countRapportsEnRetard(@Param("pointDeVenteId") Long pointDeVenteId, @Param("seuil24h") LocalDateTime seuil24h);
}
