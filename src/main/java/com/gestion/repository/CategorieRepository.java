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
}
