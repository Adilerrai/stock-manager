package com.gestion.repository;

import com.gestion.persistent.model.ChargeAchat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChargeAchatRepository extends JpaRepository<ChargeAchat, Long> {

    List<ChargeAchat> findByCommande_Id(Long commandeId);

    List<ChargeAchat> findByCommande_IdAndPointDeVenteId(Long commandeId, Long pointDeVenteId);

    Optional<ChargeAchat> findByIdAndPointDeVenteId(Long id, Long pointDeVenteId);

    List<ChargeAchat> findByLigneCommande_Id(Long ligneCommandeId);

    @Query("SELECT c FROM ChargeAchat c WHERE c.commande.id = :commandeId ORDER BY c.typeCharge ASC, c.dateCharge ASC")
    List<ChargeAchat> findByCommandeOrderByType(@Param("commandeId") Long commandeId);

    void deleteByCommande_Id(Long commandeId);
}
