package com.gestion.repository;

import com.gestion.persistent.model.Categorie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface CategorieRepository extends JpaRepository<Categorie, Long> {

    List<Categorie> findByPointDeVenteIdOrderByNomAsc(Long pointDeVenteId);

    List<Categorie> findByPointDeVenteIdAndActifTrueOrderByNomAsc(Long pointDeVenteId);

    List<Categorie> findByActifTrueOrderByNomAsc();

    // Méthodes pour la hiérarchie récursive (catégories & sous-catégories)
    List<Categorie> findByParentIsNullOrderByNomAsc();

    List<Categorie> findByParentIsNullAndPointDeVenteIdOrderByNomAsc(Long pointDeVenteId);

    @Query("SELECT c FROM Categorie c WHERE c.parent.id = :parentId ORDER BY c.nom ASC")
    List<Categorie> findByParentIdOrderByNomAsc(@Param("parentId") Long parentId);

    @Query("SELECT c FROM Categorie c WHERE c.parent.id = :parentId AND c.pointDeVenteId = :pointDeVenteId ORDER BY c.nom ASC")
    List<Categorie> findByParentIdAndPointDeVenteIdOrderByNomAsc(@Param("parentId") Long parentId, @Param("pointDeVenteId") Long pointDeVenteId);
}
