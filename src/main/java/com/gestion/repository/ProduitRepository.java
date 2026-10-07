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

    @Query("SELECT p FROM Produit p LEFT JOIN FETCH p.image WHERE p.pointDeVenteId = :pointDeVenteId")
    @Transactional(readOnly = true)
    List<Produit> findWithImagesByPointDeVenteId(@Param("pointDeVenteId") Long pointDeVenteId);

    Optional<Produit> findByIdAndPointDeVenteId(Long id, Long pointDeVenteId);

    @Query("SELECT p FROM Produit p LEFT JOIN FETCH p.image WHERE p.id = :id AND p.pointDeVenteId = :pointDeVenteId")
    Optional<Produit> findWithImageByIdAndPointDeVenteId(@Param("id") Long id, @Param("pointDeVenteId") Long pointDeVenteId);

    @Query("SELECT p FROM Produit p WHERE p.categorie.id = :categorieId")
    List<Produit> findByCategorieId(@Param("categorieId") Long categorieId);

    @Query("SELECT p FROM Produit p WHERE p.categorie.id IN :categorieIds")
    List<Produit> findByCategorieIdIn(@Param("categorieIds") List<Long> categorieIds);

    @Query("SELECT COUNT(p) FROM Produit p WHERE p.categorie.id = :categorieId")
    long countByCategorieId(@Param("categorieId") Long categorieId);

    long countByPointDeVenteId(Long pointDeVenteId);

    List<Produit> findByPointDeVenteId(Long pointDeVenteId);

    Optional<Produit> findByReferenceAndPointDeVenteId(String reference, Long pointDeVenteId);

    Optional<Produit> findFirstByReference(String reference);
}

