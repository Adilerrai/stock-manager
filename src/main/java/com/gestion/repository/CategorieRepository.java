package com.gestion.repository;

import com.gestion.persistent.model.Categorie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategorieRepository extends JpaRepository<Categorie, Long> {

    List<Categorie> findByPointDeVenteIdOrderByNomAsc(Long pointDeVenteId);

    List<Categorie> findByPointDeVenteIdAndActifTrueOrderByNomAsc(Long pointDeVenteId);

    List<Categorie> findByActifTrueOrderByNomAsc();

    // Méthodes pour la hiérarchie récursive (catégories & sous-catégories)
    List<Categorie> findByParentIsNullOrderByNomAsc();

    List<Categorie> findByParentIsNullAndPointDeVenteIdOrderByNomAsc(Long pointDeVenteId);

    List<Categorie> findByParentIdOrderByNomAsc(Long parentId);

    List<Categorie> findByParentIdAndPointDeVenteIdOrderByNomAsc(Long parentId, Long pointDeVenteId);
}
