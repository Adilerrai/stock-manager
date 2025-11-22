package com.ceramique.repository;

import com.ceramique.persistent.model.LigneLivraison;
import com.ceramique.persistent.model.Livraison;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LigneLivraisonRepository extends JpaRepository<LigneLivraison, Long> {
    List<LigneLivraison> findByLivraison_Id(Long livraisonId);
}
