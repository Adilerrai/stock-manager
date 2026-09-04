package com.gestion.repository;

import com.gestion.persistent.enums.StatutFacture;
import com.gestion.persistent.model.Facture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FactureRepository extends JpaRepository<Facture, Long>, FactureRepositoryCustom {


    Optional<Facture> findByNumeroFacture(String numeroFacture);

    List<Facture> findByPointDeVenteIdOrderByDateFactureDesc(Long pointDeVenteId);

    List<Facture> findByClientIdAndPointDeVenteId(Long clientId, Long pointDeVenteId);

    List<Facture> findByClientId(Long clientId);

    List<Facture> findByStatutAndPointDeVenteId(StatutFacture statut, Long pointDeVenteId);

    List<Facture> findByStatut(StatutFacture statut);

    @Query("SELECT f FROM Facture f WHERE f.pointDeVenteId = :pointDeVenteId AND f.dateFacture BETWEEN :dateDebut AND :dateFin ORDER BY f.dateFacture DESC")
    List<Facture> findFacturesByPeriodeAndPointDeVenteId(@Param("dateDebut") LocalDate dateDebut,
                                                         @Param("dateFin") LocalDate dateFin,
                                                         @Param("pointDeVenteId") Long pointDeVenteId);

    @Query("SELECT f FROM Facture f WHERE f.pointDeVenteId = :pointDeVenteId AND f.montantRestant > 0 AND (f.annulee = false OR f.annulee IS NULL) ORDER BY f.dateFacture DESC")
    List<Facture> findFacturesImpayeesByPointDeVenteId(@Param("pointDeVenteId") Long pointDeVenteId);

    @Query("SELECT f FROM Facture f WHERE f.pointDeVenteId = :pointDeVenteId AND f.dateEcheance < :date AND f.montantRestant > 0 AND (f.annulee = false OR f.annulee IS NULL) ORDER BY f.dateEcheance ASC")
    List<Facture> findFacturesEchuesByPointDeVenteId(@Param("date") LocalDate date, @Param("pointDeVenteId") Long pointDeVenteId);

    @Query("SELECT COALESCE(SUM(f.montantRestant), 0) FROM Facture f WHERE f.pointDeVenteId = :pointDeVenteId AND f.montantRestant > 0 AND (f.annulee = false OR f.annulee IS NULL)")
    BigDecimal sumTotalCreancesByPointDeVenteId(@Param("pointDeVenteId") Long pointDeVenteId);

    @Query("SELECT COALESCE(SUM(f.montantRestant), 0) FROM Facture f WHERE f.pointDeVenteId = :pointDeVenteId AND f.dateEcheance < :date AND f.montantRestant > 0 AND (f.annulee = false OR f.annulee IS NULL)")
    BigDecimal sumCreancesEchuesByPointDeVenteId(@Param("date") LocalDate date, @Param("pointDeVenteId") Long pointDeVenteId);

    @Query("SELECT COUNT(f) FROM Facture f WHERE f.pointDeVenteId = :pointDeVenteId AND f.montantRestant > 0 AND (f.annulee = false OR f.annulee IS NULL)")
    Long countFacturesImpayeesByPointDeVenteId(@Param("pointDeVenteId") Long pointDeVenteId);

    // Fallbacks généraux
    @Query("SELECT f FROM Facture f WHERE f.dateFacture BETWEEN :dateDebut AND :dateFin ORDER BY f.dateFacture DESC")
    List<Facture> findFacturesByPeriode(@Param("dateDebut") LocalDate dateDebut, @Param("dateFin") LocalDate dateFin);

    @Query("SELECT f FROM Facture f WHERE f.montantRestant > 0 AND (f.annulee = false OR f.annulee IS NULL) ORDER BY f.dateFacture DESC")
    List<Facture> findFacturesImpayees();

    @Query("SELECT f FROM Facture f WHERE f.dateEcheance < :date AND f.montantRestant > 0 AND (f.annulee = false OR f.annulee IS NULL) ORDER BY f.dateEcheance ASC")
    List<Facture> findFacturesEchues(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(f.montantRestant), 0) FROM Facture f WHERE f.montantRestant > 0 AND (f.annulee = false OR f.annulee IS NULL)")
    BigDecimal sumTotalCreances();

    @Query("SELECT COALESCE(SUM(f.montantRestant), 0) FROM Facture f WHERE f.dateEcheance < :date AND f.montantRestant > 0 AND (f.annulee = false OR f.annulee IS NULL)")
    BigDecimal sumCreancesEchues(@Param("date") LocalDate date);

    @Query("SELECT COUNT(f) FROM Facture f WHERE f.montantRestant > 0 AND (f.annulee = false OR f.annulee IS NULL)")
    Long countFacturesImpayees();

    @Query("SELECT COALESCE(SUM(f.montantRestant), 0) FROM Facture f WHERE f.client.commercial.id = :commercialId AND f.montantRestant > 0 AND (f.annulee = false OR f.annulee IS NULL)")
    BigDecimal sumImpayesByCommercial(@Param("commercialId") Long commercialId);
}


