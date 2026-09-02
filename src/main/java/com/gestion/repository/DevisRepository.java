package com.gestion.repository;

import com.gestion.persistent.enums.StatutDevis;
import com.gestion.persistent.model.Devis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DevisRepository extends JpaRepository<Devis, Long> {

    Optional<Devis> findByNumeroDevis(String numeroDevis);

    List<Devis> findByClientIdOrderByDateDevisDesc(Long clientId);

    List<Devis> findByStatutOrderByDateDevisDesc(StatutDevis statut);

    List<Devis> findByPointDeVenteIdOrderByDateDevisDesc(Long pointDeVenteId);

    @Query("SELECT d FROM Devis d WHERE d.dateDevis BETWEEN :dateDebut AND :dateFin ORDER BY d.dateDevis DESC")
    List<Devis> findDevisByPeriode(@Param("dateDebut") LocalDate dateDebut, @Param("dateFin") LocalDate dateFin);

    @Query("SELECT COUNT(d) FROM Devis d")
    long countAllDevis();
}
