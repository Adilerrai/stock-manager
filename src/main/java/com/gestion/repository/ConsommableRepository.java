package com.gestion.repository;

import com.gestion.persistent.enums.CategorieConsommable;
import com.gestion.persistent.model.Consommable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsommableRepository extends JpaRepository<Consommable, Long> {

    List<Consommable> findByPointDeVenteIdOrderByDesignationAsc(Long pointDeVenteId);

    List<Consommable> findByPointDeVenteIdAndActifTrueOrderByDesignationAsc(Long pointDeVenteId);

    List<Consommable> findByPointDeVenteIdAndCategorieOrderByDesignationAsc(Long pointDeVenteId, CategorieConsommable categorie);

    Optional<Consommable> findByIdAndPointDeVenteId(Long id, Long pointDeVenteId);

    Optional<Consommable> findByPointDeVenteIdAndReference(Long pointDeVenteId, String reference);

    boolean existsByPointDeVenteIdAndReference(Long pointDeVenteId, String reference);

    @Query("SELECT c FROM Consommable c WHERE c.pointDeVenteId = :pointDeVenteId AND c.actif = true AND c.stockActuel <= c.stockMinimum ORDER BY c.stockActuel ASC")
    List<Consommable> findAlertesStock(@Param("pointDeVenteId") Long pointDeVenteId);

    long countByPointDeVenteId(Long pointDeVenteId);
}
