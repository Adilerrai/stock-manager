package com.gestion.repository;

import com.gestion.persistent.enums.StatutAvoir;
import com.gestion.persistent.enums.TypeAvoir;
import com.gestion.persistent.model.Avoir;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AvoirRepository extends JpaRepository<Avoir, Long> {

    Optional<Avoir> findByNumeroAvoir(String numeroAvoir);

    List<Avoir> findByTypeAvoirOrderByDateAvoirDesc(TypeAvoir typeAvoir);

    List<Avoir> findByClientIdOrderByDateAvoirDesc(Long clientId);

    List<Avoir> findByFournisseurIdOrderByDateAvoirDesc(Long fournisseurId);

    List<Avoir> findByStatutOrderByDateAvoirDesc(StatutAvoir statut);

    List<Avoir> findByPointDeVenteIdOrderByDateAvoirDesc(Long pointDeVenteId);

    @Query("SELECT a FROM Avoir a WHERE a.dateAvoir BETWEEN :dateDebut AND :dateFin ORDER BY a.dateAvoir DESC")
    List<Avoir> findAvoirsByPeriode(@Param("dateDebut") LocalDate dateDebut, @Param("dateFin") LocalDate dateFin);

    @Query("SELECT COUNT(a) FROM Avoir a WHERE a.typeAvoir = :typeAvoir")
    long countByTypeAvoir(@Param("typeAvoir") TypeAvoir typeAvoir);

    @Query("SELECT COALESCE(SUM(a.montantTTC), 0) FROM Avoir a WHERE a.typeAvoir = :typeAvoir AND a.statut != 'ANNULE' AND a.dateAvoir BETWEEN :dateDebut AND :dateFin")
    java.math.BigDecimal sumMontantByPeriodeAndType(@Param("typeAvoir") TypeAvoir typeAvoir,
                                                    @Param("dateDebut") LocalDate dateDebut,
                                                    @Param("dateFin") LocalDate dateFin);

    @Query("SELECT la.motifRetour, COUNT(la), SUM(la.montantTTC) FROM LigneAvoir la " +
           "WHERE la.avoir.typeAvoir = 'CLIENT' AND la.avoir.statut != 'ANNULE' " +
           "AND la.avoir.dateAvoir BETWEEN :dateDebut AND :dateFin " +
           "GROUP BY la.motifRetour ORDER BY SUM(la.montantTTC) DESC")
    List<Object[]> findStatsCausesRetour(@Param("dateDebut") LocalDate dateDebut,
                                         @Param("dateFin") LocalDate dateFin);
}
