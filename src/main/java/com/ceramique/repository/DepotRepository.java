package com.ceramique.repository;

import com.ceramique.persistent.model.Depot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepotRepository extends JpaRepository<Depot, Long> {
    List<Depot> findByPointDeVente_IdAndActifTrue(Long pointDeVenteId);
    Optional<Depot> findByIdAndPointDeVente_Id(Long id, Long pointDeVenteId);
    Optional<Depot> findFirstByPointDeVente_IdAndActifTrue(Long pointDeVenteId);

    List<Depot> findByActifTrue(Boolean actif);

    boolean existsByNom(String nom);
}
