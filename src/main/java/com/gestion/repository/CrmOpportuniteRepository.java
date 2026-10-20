package com.gestion.repository;

import com.gestion.persistent.enums.EtapeOpportunite;
import com.gestion.persistent.model.CrmOpportunite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CrmOpportuniteRepository extends JpaRepository<CrmOpportunite, Long> {

    List<CrmOpportunite> findByPointDeVenteIdOrderByDateCreationDesc(Long pointDeVenteId);

    @Query("SELECT o FROM CrmOpportunite o WHERE o.pointDeVenteId = :pointDeVenteId AND o.commercial.id = :commercialId ORDER BY o.dateCreation DESC")
    List<CrmOpportunite> findByPointDeVenteIdAndCommercialIdOrderByDateCreationDesc(@Param("pointDeVenteId") Long pointDeVenteId, @Param("commercialId") Long commercialId);

    List<CrmOpportunite> findByPointDeVenteIdAndEtape(Long pointDeVenteId, EtapeOpportunite etape);

    @Query("SELECT o FROM CrmOpportunite o WHERE o.pointDeVenteId = :pointDeVenteId AND o.entreprise.id = :entrepriseId")
    List<CrmOpportunite> findByPointDeVenteIdAndEntrepriseId(@Param("pointDeVenteId") Long pointDeVenteId, @Param("entrepriseId") Long entrepriseId);

    Optional<CrmOpportunite> findByIdAndPointDeVenteId(Long id, Long pointDeVenteId);

    @Query("SELECT o FROM CrmOpportunite o WHERE o.pointDeVenteId = :pointDeVenteId AND " +
           "(LOWER(o.titre) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR (o.entreprise IS NOT NULL AND LOWER(o.entreprise.nom) LIKE LOWER(CONCAT('%', :query, '%'))))")
    List<CrmOpportunite> searchOpportunites(@Param("query") String query, @Param("pointDeVenteId") Long pointDeVenteId);

    long countByPointDeVenteId(Long pointDeVenteId);

    @Query("SELECT COUNT(o) FROM CrmOpportunite o WHERE o.pointDeVenteId = :pointDeVenteId AND o.commercial.id = :commercialId")
    long countByPointDeVenteIdAndCommercialId(@Param("pointDeVenteId") Long pointDeVenteId, @Param("commercialId") Long commercialId);

    @Query("SELECT COUNT(o) FROM CrmOpportunite o WHERE o.pointDeVenteId = :pointDeVenteId AND o.entreprise.id = :entrepriseId")
    long countByPointDeVenteIdAndEntrepriseId(@Param("pointDeVenteId") Long pointDeVenteId, @Param("entrepriseId") Long entrepriseId);

    long countByPointDeVenteIdAndEtape(Long pointDeVenteId, EtapeOpportunite etape);

    @Query("SELECT COALESCE(SUM(o.montantEstime), 0) FROM CrmOpportunite o " +
           "WHERE o.pointDeVenteId = :pointDeVenteId AND o.etape NOT IN ('GAGNE', 'PERDU')")
    BigDecimal sumMontantPipelineEnCours(@Param("pointDeVenteId") Long pointDeVenteId);

    @Query("SELECT COALESCE(SUM(o.montantEstime), 0) FROM CrmOpportunite o " +
           "WHERE o.pointDeVenteId = :pointDeVenteId AND o.etape = 'GAGNE' " +
           "AND o.dateClotureEffective >= :debutMois")
    BigDecimal sumMontantGagneMois(@Param("pointDeVenteId") Long pointDeVenteId, @Param("debutMois") LocalDateTime debutMois);
}
