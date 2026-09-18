package com.gestion.repository;

import com.gestion.persistent.enums.StatutImmobilisation;
import com.gestion.persistent.model.Immobilisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImmobilisationRepository extends JpaRepository<Immobilisation, Long> {

    List<Immobilisation> findByPointDeVenteIdOrderByCodeAsc(Long pointDeVenteId);

    Optional<Immobilisation> findByIdAndPointDeVenteId(Long id, Long pointDeVenteId);

    Optional<Immobilisation> findByCodeAndPointDeVenteId(String code, Long pointDeVenteId);

    List<Immobilisation> findByPointDeVenteIdAndStatut(Long pointDeVenteId, StatutImmobilisation statut);

    @Query("SELECT i FROM Immobilisation i WHERE i.pointDeVenteId = :pointDeVenteId AND YEAR(i.dateAcquisition) <= :annee")
    List<Immobilisation> findActifsJusquAAnnee(@Param("pointDeVenteId") Long pointDeVenteId, @Param("annee") int annee);

    @Query("SELECT COUNT(i) FROM Immobilisation i WHERE i.pointDeVenteId = :tenantId AND i.code LIKE :prefix%")
    Long countByPrefixAndTenant(@Param("prefix") String prefix, @Param("tenantId") Long tenantId);
}
