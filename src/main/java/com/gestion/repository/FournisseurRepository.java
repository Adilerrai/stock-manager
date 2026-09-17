package com.gestion.repository;

import com.gestion.persistent.model.Fournisseur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FournisseurRepository extends JpaRepository<Fournisseur, Long>, FournisseurRepositoryCustom {

    List<Fournisseur> findByActifTrue();
    
    List<Fournisseur> findByPointDeVenteIdAndActifTrue(Long pointDeVenteId);

    @Query("SELECT f FROM Fournisseur f WHERE f.actif = true ORDER BY f.raisonSociale ASC")
    List<Fournisseur> findActiveOrderByNom();

    @Query("SELECT f FROM Fournisseur f WHERE f.actif = true AND f.pointDeVenteId = :pointDeVenteId ORDER BY f.raisonSociale ASC")
    List<Fournisseur> findActiveByPointDeVenteIdOrderByNom(@Param("pointDeVenteId") Long pointDeVenteId);

    Optional<Fournisseur> findByIdAndPointDeVenteId(Long id, Long pointDeVenteId);

    boolean existsByRaisonSociale(String raisonSociale);

    boolean existsByRaisonSocialeAndPointDeVenteId(String raisonSociale, Long pointDeVenteId);
}
