package com.gestion.repository;

import com.gestion.persistent.model.DemandeAchat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DemandeAchatRepository extends JpaRepository<DemandeAchat, Long> {
    List<DemandeAchat> findByPointDeVenteIdOrderByDateDemandeDesc(Long pointDeVenteId);
    List<DemandeAchat> findByPointDeVenteIdAndStatutOrderByDateDemandeDesc(Long pointDeVenteId, com.gestion.persistent.enums.StatutDemandeAchat statut);
    Optional<DemandeAchat> findByIdAndPointDeVenteId(Long id, Long pointDeVenteId);
    Optional<DemandeAchat> findByNumeroDaAndPointDeVenteId(String numeroDa, Long pointDeVenteId);
}

