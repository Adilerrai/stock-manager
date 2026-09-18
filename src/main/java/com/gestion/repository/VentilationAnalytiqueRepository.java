package com.gestion.repository;

import com.gestion.persistent.model.VentilationAnalytique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VentilationAnalytiqueRepository extends JpaRepository<VentilationAnalytique, Long> {
    List<VentilationAnalytique> findByLigneEcritureId(Long ligneEcritureId);
    void deleteByLigneEcritureId(Long ligneEcritureId);
    List<VentilationAnalytique> findByCentreAnalytiqueId(Long centreAnalytiqueId);

    @Query("SELECT v FROM VentilationAnalytique v JOIN LigneEcriture l ON v.ligneEcritureId = l.id JOIN l.ecriture e " +
           "WHERE v.centreAnalytique.id = :centreId AND v.pointDeVenteId = :tenantId AND e.dateEcriture BETWEEN :dateDebut AND :dateFin")
    List<VentilationAnalytique> findByCentreAndPeriode(@Param("centreId") Long centreId,
                                                       @Param("tenantId") Long tenantId,
                                                       @Param("dateDebut") LocalDate dateDebut,
                                                       @Param("dateFin") LocalDate dateFin);
}
