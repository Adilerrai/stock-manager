package com.gestion.repository;

import com.gestion.persistent.enums.StatutLivraison;
import com.gestion.persistent.model.BonLivraisonClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BonLivraisonClientRepository extends JpaRepository<BonLivraisonClient, Long>, BonLivraisonClientRepositoryCustom {
    List<BonLivraisonClient> findByPointDeVenteId(Long pointDeVenteId);
    Optional<BonLivraisonClient> findByIdAndPointDeVenteId(Long id, Long pointDeVenteId);
    Optional<BonLivraisonClient> findByIdAndClientId(Long id, Long clientId);
    List<BonLivraisonClient> findByClientIdAndPointDeVenteId(Long clientId, Long pointDeVenteId);
    List<BonLivraisonClient> findByClientIdOrderByDateBlDesc(Long clientId);
    List<BonLivraisonClient> findByCommandeClientIdAndPointDeVenteId(Long commandeClientId, Long pointDeVenteId);
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT bl FROM BonLivraisonClient bl LEFT JOIN bl.commandes c WHERE bl.commandeClient.id = :commandeId OR c.id = :commandeId")
    List<BonLivraisonClient> findAllByCommandeId(@org.springframework.data.repository.query.Param("commandeId") Long commandeId);
    List<BonLivraisonClient> findByStatutAndPointDeVenteId(StatutLivraison statut, Long pointDeVenteId);
    List<BonLivraisonClient> findByClientIdAndFactureIsNullAndPointDeVenteId(Long clientId, Long pointDeVenteId);
    List<BonLivraisonClient> findByFactureIsNullAndPointDeVenteId(Long pointDeVenteId);
    List<BonLivraisonClient> findByFactureId(Long factureId);
    boolean existsByNumeroBl(String numeroBl);
    Optional<BonLivraisonClient> findByPointDeVenteIdAndNumeroBl(Long pointDeVenteId, String numeroBl);
}
