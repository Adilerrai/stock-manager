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

    List<Fournisseur> findByPointDeVente_IdAndActifTrue(Long pointDeVenteId);
    
    Optional<Fournisseur> findByIdAndPointDeVente_Id(Long id, Long pointDeVenteId);
    
    boolean existsByRaisonSocialeAndPointDeVente_Id(String nom, Long pointDeVenteId);
    
    @Query("SELECT f FROM Fournisseur f WHERE f.pointDeVente.id = :pointDeVenteId AND f.actif = true ORDER BY f.raisonSociale ASC")
    List<Fournisseur> findActiveByPointDeVenteOrderByNom(@Param("pointDeVenteId") Long pointDeVenteId);
}