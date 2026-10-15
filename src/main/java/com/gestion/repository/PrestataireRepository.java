package com.gestion.repository;

import com.gestion.persistent.model.Prestataire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrestataireRepository extends JpaRepository<Prestataire, Long> {

    List<Prestataire> findByPointDeVenteIdOrderByRaisonSocialeAsc(Long pointDeVenteId);

    List<Prestataire> findByPointDeVenteIdAndActifTrueOrderByRaisonSocialeAsc(Long pointDeVenteId);

    Optional<Prestataire> findByIdAndPointDeVenteId(Long id, Long pointDeVenteId);

    boolean existsByPointDeVenteIdAndRaisonSocialeIgnoreCase(Long pointDeVenteId, String raisonSociale);

    long countByPointDeVenteId(Long pointDeVenteId);
}
