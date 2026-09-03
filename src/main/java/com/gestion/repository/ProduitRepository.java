package com.gestion.repository;

import com.gestion.persistent.model.Produit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long>, ProduitRepositoryCustom {
    


    @Query("SELECT p FROM Produit p LEFT JOIN FETCH p.image ")
    @Transactional(readOnly = true)
    List<Produit> findWithImages();

    List<Produit> findByCategorieId(Long categorieId);

    List<Produit> findByCategorieIdIn(List<Long> categorieIds);

    long countByCategorieId(Long categorieId);
}

