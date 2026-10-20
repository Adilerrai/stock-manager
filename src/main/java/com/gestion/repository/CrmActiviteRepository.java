package com.gestion.repository;

import com.gestion.persistent.model.CrmActivite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CrmActiviteRepository extends JpaRepository<CrmActivite, Long> {

    List<CrmActivite> findByPointDeVenteIdOrderByDateActiviteDesc(Long pointDeVenteId);

    @Query("SELECT a FROM CrmActivite a WHERE a.pointDeVenteId = :pointDeVenteId AND a.entreprise.id = :entrepriseId ORDER BY a.dateActivite DESC")
    List<CrmActivite> findByPointDeVenteIdAndEntrepriseIdOrderByDateActiviteDesc(@Param("pointDeVenteId") Long pointDeVenteId, @Param("entrepriseId") Long entrepriseId);

    @Query("SELECT a FROM CrmActivite a WHERE a.pointDeVenteId = :pointDeVenteId AND a.contact.id = :contactId ORDER BY a.dateActivite DESC")
    List<CrmActivite> findByPointDeVenteIdAndContactIdOrderByDateActiviteDesc(@Param("pointDeVenteId") Long pointDeVenteId, @Param("contactId") Long contactId);

    @Query("SELECT a FROM CrmActivite a WHERE a.pointDeVenteId = :pointDeVenteId AND a.opportunite.id = :opportuniteId ORDER BY a.dateActivite DESC")
    List<CrmActivite> findByPointDeVenteIdAndOpportuniteIdOrderByDateActiviteDesc(@Param("pointDeVenteId") Long pointDeVenteId, @Param("opportuniteId") Long opportuniteId);

    @Query("SELECT a FROM CrmActivite a WHERE a.pointDeVenteId = :pointDeVenteId AND a.visite.id = :visiteId ORDER BY a.dateActivite DESC")
    List<CrmActivite> findByPointDeVenteIdAndVisiteIdOrderByDateActiviteDesc(@Param("pointDeVenteId") Long pointDeVenteId, @Param("visiteId") Long visiteId);
}
