package com.ceramique.repository;

import com.ceramique.persistent.enums.StatutFacture;
import com.ceramique.persistent.model.FactureAchat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FactureAchatRepository extends JpaRepository<FactureAchat, Long> {
    List<FactureAchat> findByPointDeVenteId(Long pointDeVenteId);
    Optional<FactureAchat> findByIdAndPointDeVenteId(Long id, Long pointDeVenteId);
    List<FactureAchat> findByFournisseurIdAndPointDeVenteId(Long fournisseurId, Long pointDeVenteId);
    List<FactureAchat> findByStatutAndPointDeVenteId(StatutFacture statut, Long pointDeVenteId);
    Optional<FactureAchat> findByNumeroFactureAndPointDeVenteId(String numeroFacture, Long pointDeVenteId);
}
