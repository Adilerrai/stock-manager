package com.ceramique.repository;

import com.ceramique.persistent.enums.StatutLivraison;
import com.ceramique.persistent.model.BonLivraisonClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BonLivraisonClientRepository extends JpaRepository<BonLivraisonClient, Long> {
    List<BonLivraisonClient> findByPointDeVenteId(Long pointDeVenteId);
    Optional<BonLivraisonClient> findByIdAndPointDeVenteId(Long id, Long pointDeVenteId);
    List<BonLivraisonClient> findByClientIdAndPointDeVenteId(Long clientId, Long pointDeVenteId);
    List<BonLivraisonClient> findByCommandeClientIdAndPointDeVenteId(Long commandeClientId, Long pointDeVenteId);
    List<BonLivraisonClient> findByStatutAndPointDeVenteId(StatutLivraison statut, Long pointDeVenteId);
}
