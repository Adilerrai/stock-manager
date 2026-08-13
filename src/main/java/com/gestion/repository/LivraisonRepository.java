package com.gestion.repository;

import com.gestion.persistent.enums.StatutLivraison;
import com.gestion.persistent.model.Commande;
import com.gestion.persistent.model.Livraison;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LivraisonRepository extends JpaRepository<Livraison, Long>, LivraisonRepositoryCustom {


    List<Livraison> findByCommande_IdAndStatut(Long commandeId, StatutLivraison statut);

    List<Livraison> findByStatut(StatutLivraison statut);
}

