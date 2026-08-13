package com.gestion.repository;

import com.gestion.persistent.model.LigneLivraison;
import com.gestion.persistent.model.Livraison;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LigneLivraisonRepository extends JpaRepository<LigneLivraison, Long> {
    List<LigneLivraison> findByLivraison_Id(Long livraisonId);
}

