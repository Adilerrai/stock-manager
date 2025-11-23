package com.ceramique.repository;

import com.ceramique.persistent.enums.StatutCommandeClient;
import com.ceramique.persistent.model.CommandeClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommandeClientRepository extends JpaRepository<CommandeClient, Long> {
    
    List<CommandeClient> findByAllOrderByDateCommandeDesc();
    
    Optional<CommandeClient> findById(Long id);

    List<CommandeClient> findByStatut(StatutCommandeClient statut);

    List<CommandeClient> findByClientNomContainingIgnoreCase(String clientNom);
    
    @Query("SELECT c FROM CommandeClient c WHERE  c.dateCommande BETWEEN :dateDebut AND :dateFin")
    List<CommandeClient> findByPointDeVenteAndDateCommandeBetween(
        @Param("pointDeVenteId") Long pointDeVenteId,
        @Param("dateDebut") LocalDateTime dateDebut,
        @Param("dateFin") LocalDateTime dateFin
    );

    boolean existsByNumeroCommande(String numeroCommande);
}