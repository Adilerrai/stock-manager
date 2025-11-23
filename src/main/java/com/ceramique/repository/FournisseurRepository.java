package com.ceramique.repository;

import com.ceramique.persistent.model.Fournisseur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FournisseurRepository extends JpaRepository<Fournisseur, Long>, FournisseurRepositoryCustom {

    List<Fournisseur> findByActifTrue();
    

    @Query("SELECT f FROM Fournisseur f WHERE f.actif = true ORDER BY f.raisonSociale ASC")
    List<Fournisseur> findActiveOrderByNom();

    boolean existsByRaisonSociale(String raisonSociale);
}