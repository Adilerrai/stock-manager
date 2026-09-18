package com.gestion.repository;

import com.gestion.persistent.model.AxeAnalytique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AxeAnalytiqueRepository extends JpaRepository<AxeAnalytique, Long> {
    List<AxeAnalytique> findByPointDeVenteIdOrderByLibelleAsc(Long pointDeVenteId);
    List<AxeAnalytique> findByPointDeVenteIdAndActifTrueOrderByLibelleAsc(Long pointDeVenteId);
    Optional<AxeAnalytique> findByIdAndPointDeVenteId(Long id, Long pointDeVenteId);
    Optional<AxeAnalytique> findByCodeAndPointDeVenteId(String code, Long pointDeVenteId);
    boolean existsByCodeAndPointDeVenteId(String code, Long pointDeVenteId);
}
