package com.gestion.repository;

import com.gestion.persistent.enums.StatutOffreFournisseur;
import com.gestion.persistent.model.OffreFournisseur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OffreFournisseurRepository extends JpaRepository<OffreFournisseur, Long> {

    List<OffreFournisseur> findByDemandePrixIdAndPointDeVenteId(Long demandePrixId, Long pointDeVenteId);

    Optional<OffreFournisseur> findByIdAndPointDeVenteId(Long id, Long pointDeVenteId);

    Optional<OffreFournisseur> findByDemandePrixIdAndFournisseurId(Long demandePrixId, Long fournisseurId);

    List<OffreFournisseur> findByFournisseurIdAndPointDeVenteId(Long fournisseurId, Long pointDeVenteId);
}
