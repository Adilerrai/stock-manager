package com.gestion.repository;

import com.gestion.persistent.model.CentreAnalytique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CentreAnalytiqueRepository extends JpaRepository<CentreAnalytique, Long> {
    List<CentreAnalytique> findByPointDeVenteIdOrderByLibelleAsc(Long pointDeVenteId);
    List<CentreAnalytique> findByAxeIdAndPointDeVenteIdOrderByLibelleAsc(Long axeId, Long pointDeVenteId);
    List<CentreAnalytique> findByAxeIdAndPointDeVenteIdAndActifTrueOrderByLibelleAsc(Long axeId, Long pointDeVenteId);
    Optional<CentreAnalytique> findByIdAndPointDeVenteId(Long id, Long pointDeVenteId);
    Optional<CentreAnalytique> findByCodeAndPointDeVenteId(String code, Long pointDeVenteId);
    boolean existsByCodeAndPointDeVenteId(String code, Long pointDeVenteId);
}
