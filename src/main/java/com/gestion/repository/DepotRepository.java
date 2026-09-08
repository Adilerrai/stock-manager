package com.gestion.repository;

import com.gestion.persistent.model.Depot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepotRepository extends JpaRepository<Depot, Long> {

    List<Depot> findByActifTrue();

    List<Depot> findByPointDeVenteIdAndActifTrue(Long pointDeVenteId);

    List<Depot> findByPointDeVenteId(Long pointDeVenteId);

    Optional<Depot> findByIdAndPointDeVenteId(Long id, Long pointDeVenteId);

    boolean existsByNom(String nom);

    boolean existsByNomAndPointDeVenteId(String nom, Long pointDeVenteId);
}

