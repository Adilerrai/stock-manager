package com.gestion.repository;

import com.gestion.persistent.enums.StatutCommande;
import com.gestion.persistent.model.Commande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long>, CommandeRepositoryCustom {
    


    List<Commande> findByStatut(StatutCommande statut);

    List<Commande> findByFournisseurId(Long fournisseurId);


    @Query("SELECT c FROM Commande c WHERE c.dateCommande BETWEEN :dateDebut AND :dateFin ORDER BY c.dateCommande DESC")
    List<Commande> findByDateRange(@Param("dateDebut") LocalDateTime dateDebut,
                                                  @Param("dateFin") LocalDateTime dateFin);
    
    boolean existsByNumeroCommande(String numeroCommande);
}
