package com.ceramique.repository;

import com.ceramique.persistent.enums.StatutVente;
import com.ceramique.persistent.model.Vente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VenteRepository extends JpaRepository<Vente, Long> {



    List<Vente> findByClientId(Long clientId);



    @Query("SELECT v FROM Vente v  " +
           "WHERE v.dateVente BETWEEN :dateDebut AND :dateFin " +
           "ORDER BY v.dateVente DESC")
    List<Vente> findVentesByPeriode(@Param("dateDebut") LocalDateTime dateDebut,
                                     @Param("dateFin") LocalDateTime dateFin);

    @Query("SELECT SUM(v.montantFinal) FROM Vente v " +
           "WHERE v.statut = 'VALIDEE' AND v.dateVente BETWEEN :dateDebut AND :dateFin")
    BigDecimal calculerChiffreAffaires(@Param("dateDebut") LocalDateTime dateDebut,
                                        @Param("dateFin") LocalDateTime dateFin);

    @Query("SELECT COUNT(v) FROM Vente v WHERE  " +
           " v.dateVente BETWEEN :dateDebut AND :dateFin")
    Long countVentesByPeriode(@Param("dateDebut") LocalDateTime dateDebut,
                               @Param("dateFin") LocalDateTime dateFin);

    @Query("SELECT v FROM Vente v WHERE " +
           " v.montantRestant > 0 " +
           "ORDER BY v.dateVente DESC")
    List<Vente> findVentesNonSoldees();
}

