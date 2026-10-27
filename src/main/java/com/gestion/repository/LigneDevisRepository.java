package com.gestion.repository;

import com.gestion.persistent.model.LigneDevis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LigneDevisRepository extends JpaRepository<LigneDevis, Long> {
    List<LigneDevis> findByDevisId(Long devisId);

    List<LigneDevis> findByStatutPrecommande(com.gestion.persistent.enums.StatutPrecommande statutPrecommande);

    @org.springframework.data.jpa.repository.Query("SELECT ld FROM LigneDevis ld JOIN ld.devis d WHERE ld.statutPrecommande = :statut AND d.pointDeVenteId = :tenantId ORDER BY ld.dateAjoutPrecommande DESC")
    List<LigneDevis> findByStatutPrecommandeAndTenantId(
            @org.springframework.data.repository.query.Param("statut") com.gestion.persistent.enums.StatutPrecommande statut,
            @org.springframework.data.repository.query.Param("tenantId") Long tenantId
    );

    List<LigneDevis> findByDemandeAchatId(Long demandeAchatId);
}

