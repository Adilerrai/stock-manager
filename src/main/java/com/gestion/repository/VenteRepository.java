package com.gestion.repository;

import com.gestion.persistent.enums.StatutVente;
import com.gestion.persistent.model.Vente;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VenteRepository extends JpaRepository<Vente, Long>, VenteRepositoryCustom {



    List<Vente> findByClientId(Long clientId);

    List<Vente> findByPointDeVenteIdOrderByDateVenteDesc(Long pointDeVenteId);

    @Query("SELECT v FROM Vente v WHERE v.pointDeVenteId = :pointDeVenteId AND v.dateVente BETWEEN :dateDebut AND :dateFin ORDER BY v.dateVente DESC")
    List<Vente> findVentesByPeriodeAndPointDeVenteId(@Param("dateDebut") LocalDateTime dateDebut,
                                                     @Param("dateFin") LocalDateTime dateFin,
                                                     @Param("pointDeVenteId") Long pointDeVenteId);

    @Query("SELECT COALESCE(SUM(v.montantFinal), 0) FROM Vente v WHERE v.pointDeVenteId = :pointDeVenteId AND v.statut = 'VALIDEE' AND v.dateVente BETWEEN :dateDebut AND :dateFin")
    BigDecimal calculerChiffreAffairesByPointDeVenteId(@Param("dateDebut") LocalDateTime dateDebut,
                                                        @Param("dateFin") LocalDateTime dateFin,
                                                        @Param("pointDeVenteId") Long pointDeVenteId);

    @Query("SELECT COUNT(v) FROM Vente v WHERE v.pointDeVenteId = :pointDeVenteId AND v.dateVente BETWEEN :dateDebut AND :dateFin")
    Long countVentesByPeriodeAndPointDeVenteId(@Param("dateDebut") LocalDateTime dateDebut,
                                               @Param("dateFin") LocalDateTime dateFin,
                                               @Param("pointDeVenteId") Long pointDeVenteId);

    @Query("SELECT v FROM Vente v WHERE v.pointDeVenteId = :pointDeVenteId AND v.montantRestant > 0 ORDER BY v.dateVente DESC")
    List<Vente> findVentesNonSoldeesByPointDeVenteId(@Param("pointDeVenteId") Long pointDeVenteId);

    @Query("SELECT v.client.id, v.client.nom, v.client.nomComplet, SUM(v.montantFinal), COUNT(v) FROM Vente v " +
           "WHERE v.pointDeVenteId = :pointDeVenteId AND v.statut = 'VALIDEE' AND v.client IS NOT NULL " +
           "GROUP BY v.client.id, v.client.nom, v.client.nomComplet " +
           "ORDER BY SUM(v.montantFinal) DESC")
    List<Object[]> findTopClientsByPointDeVenteId(@Param("pointDeVenteId") Long pointDeVenteId, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(v) FROM Vente v WHERE v.pointDeVenteId = :pointDeVenteId AND v.statut = :statut AND v.dateVente BETWEEN :dateDebut AND :dateFin")
    Long countVentesByStatutAndPeriodeAndPointDeVenteId(@Param("statut") StatutVente statut,
                                                        @Param("dateDebut") LocalDateTime dateDebut,
                                                        @Param("dateFin") LocalDateTime dateFin,
                                                        @Param("pointDeVenteId") Long pointDeVenteId);

    // Fallbacks généraux
    @Query("SELECT v FROM Vente v WHERE v.dateVente BETWEEN :dateDebut AND :dateFin ORDER BY v.dateVente DESC")
    List<Vente> findVentesByPeriode(@Param("dateDebut") LocalDateTime dateDebut, @Param("dateFin") LocalDateTime dateFin);

    @Query("SELECT SUM(v.montantFinal) FROM Vente v WHERE v.statut = 'VALIDEE' AND v.dateVente BETWEEN :dateDebut AND :dateFin")
    BigDecimal calculerChiffreAffaires(@Param("dateDebut") LocalDateTime dateDebut, @Param("dateFin") LocalDateTime dateFin);

    @Query("SELECT COUNT(v) FROM Vente v WHERE v.dateVente BETWEEN :dateDebut AND :dateFin")
    Long countVentesByPeriode(@Param("dateDebut") LocalDateTime dateDebut, @Param("dateFin") LocalDateTime dateFin);

    @Query("SELECT v FROM Vente v WHERE v.montantRestant > 0 ORDER BY v.dateVente DESC")
    List<Vente> findVentesNonSoldees();

    @Query("SELECT v.client.id, v.client.nom, v.client.nomComplet, SUM(v.montantFinal), COUNT(v) FROM Vente v " +
           "WHERE v.statut = 'VALIDEE' AND v.client IS NOT NULL " +
           "GROUP BY v.client.id, v.client.nom, v.client.nomComplet " +
           "ORDER BY SUM(v.montantFinal) DESC")
    List<Object[]> findTopClients(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(v) FROM Vente v WHERE v.statut = :statut AND v.dateVente BETWEEN :dateDebut AND :dateFin")
    Long countVentesByStatutAndPeriode(@Param("statut") StatutVente statut,
                                        @Param("dateDebut") LocalDateTime dateDebut,
                                        @Param("dateFin") LocalDateTime dateFin);

    @Query("SELECT COALESCE(SUM(v.montantFinal), 0) FROM Vente v WHERE (v.vendeur.id = :commercialId OR v.client.commercial.id = :commercialId) AND v.statut = 'VALIDEE' AND v.dateVente BETWEEN :debut AND :fin")
    BigDecimal sumCAByCommercial(@Param("commercialId") Long commercialId,
                                 @Param("debut") LocalDateTime debut,
                                 @Param("fin") LocalDateTime fin);

    @Query("SELECT COUNT(v) FROM Vente v WHERE (v.vendeur.id = :commercialId OR v.client.commercial.id = :commercialId) AND v.statut = 'VALIDEE' AND v.dateVente BETWEEN :debut AND :fin")
    Long countVentesByCommercial(@Param("commercialId") Long commercialId,
                                 @Param("debut") LocalDateTime debut,
                                 @Param("fin") LocalDateTime fin);
}


