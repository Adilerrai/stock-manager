package com.gestion.repository;

import com.gestion.persistent.enums.StatutFacture;
import com.gestion.persistent.model.FactureAchat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FactureAchatRepository extends JpaRepository<FactureAchat, Long> {
    List<FactureAchat> findByPointDeVenteId(Long pointDeVenteId);
    Optional<FactureAchat> findByIdAndPointDeVenteId(Long id, Long pointDeVenteId);
    List<FactureAchat> findByFournisseurIdAndPointDeVenteId(Long fournisseurId, Long pointDeVenteId);
    List<FactureAchat> findByFournisseurId(Long fournisseurId);
    List<FactureAchat> findByStatutAndPointDeVenteId(StatutFacture statut, Long pointDeVenteId);
    Optional<FactureAchat> findByNumeroFactureAndPointDeVenteId(String numeroFacture, Long pointDeVenteId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(f.montantTtc), 0) FROM FactureAchat f WHERE f.statut != 'PAYEE_TOTALEMENT' AND f.statut != 'ANNULEE'")
    java.math.BigDecimal sumTotalDettesFournisseurs();
}

