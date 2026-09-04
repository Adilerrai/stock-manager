package com.gestion.repository;

import com.gestion.persistent.model.CompteComptable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompteComptableRepository extends JpaRepository<CompteComptable, Long> {

    List<CompteComptable> findByPointDeVenteIdOrderByNumeroCompteAsc(Long pointDeVenteId);

    List<CompteComptable> findByPointDeVenteIdAndClasseOrderByNumeroCompteAsc(Long pointDeVenteId, Integer classe);

    Optional<CompteComptable> findByNumeroCompteAndPointDeVenteId(String numeroCompte, Long pointDeVenteId);

    List<CompteComptable> findByPointDeVenteIdAndActifTrueOrderByNumeroCompteAsc(Long pointDeVenteId);

    boolean existsByNumeroCompteAndPointDeVenteId(String numeroCompte, Long pointDeVenteId);
}
